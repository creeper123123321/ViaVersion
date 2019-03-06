package us.myles.ViaVersion.protocols.protocol1_13to1_12_2.storage;

import lombok.Synchronized;
import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.Position;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class BlockConnectionStorage extends StoredObject {
    private Map<Long, Pair<String, char[]>> blockStorage = createLongObjectMap();

    private static Constructor<?> fastUtilLongObjectHashMap;

    static {
        try {
            fastUtilLongObjectHashMap = Class.forName("it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap").getConstructor();
            Via.getPlatform().getLogger().info("Using FastUtil Long2ObjectOpenHashMap for block connections");
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
        Via.getPlatform().runRepeatingSync(new Runnable() {
            @Override
            public void run() {
                Via.getPlatform().runAsync(new Runnable() {
                    @Override
                    public void run() {
                        for (UserConnection info : Via.getManager().getPortedPlayers().values()) {
                            BlockConnectionStorage storage = info.get(BlockConnectionStorage.class);
                            if (storage != null) {
                                if (info.getChannel().isOpen()) {
                                    storage.transformToString();
                                }
                            }
                        }
                    }
                });
            }
        }, 20L);
    }

    public BlockConnectionStorage(UserConnection user) {
        super(user);
    }

    @Synchronized
    public void store(Position position, int blockState) {
        long pair = getChunkSectionIndex(position);
        Pair<String, char[]> map = getChunkSection(pair);
        int blockIndex = encodeBlockPos(position);
        char[] chars;
        if (map.getKey() != null) {
            chars = map.getKey().toCharArray();
            map.setValue(chars);
            map.setKey(null);
        } else {
            chars = map.getValue();
        }
        chars[blockIndex] = (char) blockState;
    }

    @Synchronized
    public int get(Position position) {
        long pair = getChunkSectionIndex(position);
        Pair<String, char[]> map = blockStorage.get(pair);
        if (map == null) return 0;
        short blockPosition = encodeBlockPos(position);
        if (map.getKey() != null) {
            return map.getKey().charAt(blockPosition);
        } else {
            return map.getValue()[blockPosition];
        }
    }

    @Synchronized
    public void remove(Position position) {
        long pair = getChunkSectionIndex(position);
        Pair<String, char[]> map = blockStorage.get(pair);
        if (map == null) return;
        int blockIndex = encodeBlockPos(position);
        char[] chars;
        if (map.getKey() != null) {
            chars = map.getKey().toCharArray();
            map.setValue(chars);
            map.setKey(null);
        } else {
            chars = map.getValue();
        }
        chars[blockIndex] = (char) 0;
        for (char block : chars) {
            if (block != 0) return;
        }
        blockStorage.remove(pair);
    }

    @Synchronized
    public void clear() {
        blockStorage.clear();
    }

    @Synchronized
    public void unloadChunk(int x, int z) {
        for (int y = 0; y < 256; y += 16) {
            blockStorage.remove(getChunkSectionIndex(x << 4, y, z << 4));
        }
    }

    @Synchronized
    public void transformToString() {
        for (Pair<String, char[]> pair : blockStorage.values()) {
            if (pair.getValue() != null) {
                pair.setKey(new String(pair.getValue()));
                pair.setValue(null);
            }
        }
    }

    private Pair<String, char[]> getChunkSection(long index) {
        Pair<String, char[]> map = blockStorage.get(index);
        if (map == null) {
            map = new Pair<>(null, new char[4096]);
            blockStorage.put(index, map);
        }
        return map;
    }

    private long getChunkSectionIndex(int x, int y, int z) {
        return (((x >> 4) & 0x3FFFFFFL) << 38) | (((y >> 4) & 0xFFFL) << 26) | ((z >> 4) & 0x3FFFFFFL);
    }

    private long getChunkSectionIndex(Position position) {
        return getChunkSectionIndex(position.getX().intValue(), position.getY().intValue(), position.getZ().intValue());
    }

    private short encodeBlockPos(int x, int y, int z) {
        return (short) (((y & 0xF) << 8) | ((x & 0xF) << 4) | (z & 0xF));
    }

    private short encodeBlockPos(Position pos) {
        return encodeBlockPos(pos.getX().intValue(), pos.getY().intValue(), pos.getZ().intValue());
    }

    private <T> Map<Long, T> createLongObjectMap() {
        if (fastUtilLongObjectHashMap != null) {
            try {
                return (Map<Long, T>) fastUtilLongObjectHashMap.newInstance();
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }
}
