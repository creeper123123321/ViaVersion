package us.myles.ViaVersion.protocols.protocol1_14to1_13_2.storage;

import com.google.common.base.Optional;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_14Types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
    private final Map<Integer, Entity1_14Types.EntityType> clientEntityTypes = new ConcurrentHashMap<>();

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public void removeEntity(int entityId) {
        clientEntityTypes.remove(entityId);
    }

    public void addEntity(int entityId, Entity1_14Types.EntityType type) {
        clientEntityTypes.put(entityId, type);
    }

    public boolean has(int entityId) {
        return clientEntityTypes.containsKey(entityId);
    }

    public Optional<Entity1_14Types.EntityType> get(int id) {
        return Optional.fromNullable(clientEntityTypes.get(id));
    }
}
