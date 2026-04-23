package com.smartcampus.api.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.exception.SensorUnavailableException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

public final class CampusStore {
    private static final CampusStore INSTANCE = new CampusStore();

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<SensorReading>> readingsBySensor = new ConcurrentHashMap<>();

    private CampusStore() {
    }

    public static CampusStore getInstance() {
        return INSTANCE;
    }

    public List<Room> getAllRooms() {
        return rooms.values().stream()
                .map(Room::new)
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());
    }

    public Room getRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' was not found.");
        }
        return new Room(room);
    }

    public Room createRoom(Room roomInput) {
        if (roomInput == null) {
            throw new BadRequestException("Room payload is required.");
        }

        String roomId = requireText(roomInput.getId(), "Room id is required.");
        String roomName = requireText(roomInput.getName(), "Room name is required.");

        if (roomInput.getCapacity() < 0) {
            throw new BadRequestException("Room capacity must be zero or greater.");
        }

        Room room = new Room(roomId, roomName, roomInput.getCapacity());
        Room existing = rooms.putIfAbsent(roomId, room);
        if (existing != null) {
            throw new WebApplicationException("Room '" + roomId + "' already exists.", Response.Status.CONFLICT);
        }

        return new Room(room);
    }

    public void deleteRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' was not found.");
        }

        synchronized (room) {
            if (!room.getSensorIds().isEmpty()) {
                throw new RoomNotEmptyException("Room '" + roomId + "' still has active sensors assigned.");
            }
            rooms.remove(roomId, room);
        }
    }

    public List<Sensor> getAllSensors(String type) {
        return sensors.values().stream()
                .filter(sensor -> type == null || type.isBlank() || sensor.getType().equalsIgnoreCase(type))
                .map(Sensor::new)
                .sorted(Comparator.comparing(Sensor::getId))
                .collect(Collectors.toList());
    }

    public Sensor getSensor(String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' was not found.");
        }
        return new Sensor(sensor);
    }

    public Sensor createSensor(Sensor sensorInput) {
        if (sensorInput == null) {
            throw new BadRequestException("Sensor payload is required.");
        }

        String sensorId = requireText(sensorInput.getId(), "Sensor id is required.");
        String type = requireText(sensorInput.getType(), "Sensor type is required.");
        String roomId = requireText(sensorInput.getRoomId(), "Sensor roomId is required.");
        Room room = rooms.get(roomId);

        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot create sensor '" + sensorId + "' because room '" + roomId + "' does not exist.");
        }

        String status = normalizeStatus(sensorInput.getStatus());
        Sensor sensor = new Sensor(sensorId, type, status, sensorInput.getCurrentValue(), roomId);
        Sensor existing = sensors.putIfAbsent(sensorId, sensor);
        if (existing != null) {
            throw new WebApplicationException("Sensor '" + sensorId + "' already exists.", Response.Status.CONFLICT);
        }

        synchronized (room) {
            if (!room.getSensorIds().contains(sensorId)) {
                room.getSensorIds().add(sensorId);
            }
        }

        readingsBySensor.putIfAbsent(sensorId, new CopyOnWriteArrayList<>());
        return new Sensor(sensor);
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        ensureSensorExists(sensorId);
        List<SensorReading> readings = readingsBySensor.getOrDefault(sensorId, new CopyOnWriteArrayList<>());
        return readings.stream()
                .map(SensorReading::new)
                .sorted(Comparator.comparingLong(SensorReading::getTimestamp))
                .collect(Collectors.toList());
    }

    public SensorReading addReading(String sensorId, SensorReading readingInput) {
        if (readingInput == null) {
            throw new BadRequestException("Sensor reading payload is required.");
        }

        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' was not found.");
        }

        synchronized (sensor) {
            if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
                throw new SensorUnavailableException(
                        "Sensor '" + sensorId + "' is in MAINTENANCE mode and cannot accept new readings.");
            }
        }

        String readingId = readingInput.getId();
        if (readingId == null || readingId.isBlank()) {
            readingId = UUID.randomUUID().toString();
        }

        long timestamp = readingInput.getTimestamp() > 0 ? readingInput.getTimestamp() : System.currentTimeMillis();
        SensorReading reading = new SensorReading(readingId, timestamp, readingInput.getValue());

        readingsBySensor.computeIfAbsent(sensorId, key -> new CopyOnWriteArrayList<>()).add(reading);

        synchronized (sensor) {
            sensor.setCurrentValue(reading.getValue());
        }

        return new SensorReading(reading);
    }

    public Map<String, Integer> counts() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        counts.put("rooms", rooms.size());
        counts.put("sensors", sensors.size());
        counts.put("readingCollections", readingsBySensor.size());
        return counts;
    }

    private void ensureSensorExists(String sensorId) {
        if (!sensors.containsKey(sensorId)) {
            throw new NotFoundException("Sensor '" + sensorId + "' was not found.");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        String normalized = status.trim().toUpperCase();
        List<String> allowed = new ArrayList<>();
        allowed.add("ACTIVE");
        allowed.add("MAINTENANCE");
        allowed.add("OFFLINE");

        if (!allowed.contains(normalized)) {
            throw new BadRequestException("Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE.");
        }

        return normalized;
    }
}
