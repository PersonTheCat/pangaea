package personthecat.pangaea.world.road;

import net.minecraft.server.level.ServerLevel;
import personthecat.pangaea.data.Point;
import personthecat.pangaea.registry.PgRegistries;
import personthecat.pangaea.util.Stopwatch;
import personthecat.pangaea.world.level.PangaeaContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoadSystem {
    private static final short QUAD_RADIUS = 2;
    private final Stopwatch sw = new Stopwatch();
    private final RoadMap map;
    private final List<? extends RoadGenerator<?>> generators;

    public RoadSystem(ServerLevel level, RoadMap map) {
        this.map = map;
        this.generators = loadGenerators(level, map);
    }

    private static List<? extends RoadGenerator<?>> loadGenerators(ServerLevel level, RoadMap map) {
        return PgRegistries.ROAD.stream()
            .map(s -> s.createGenerator(level, map))
            .toList();
    }

    public Map<Point, RoadRegion> populateRegion(PangaeaContext ctx, RoadRegion region, boolean partial) {
        this.sw.logStart("Generating road map %s, %s", region.x, region.z);
        final var generated = this.populate(ctx, region, region.x, region.z, partial);
        this.sw.logEnd("Map done %s, %s", region.x, region.z);
        this.sw.logAverage("Average");
        return generated;
    }

    private Map<Point, RoadRegion> populate(PangaeaContext ctx, RoadRegion region, short x, short z, boolean partial) {
        final Map<Point, RoadRegion> generated = new HashMap<>();
        final short cQX = (short) ((x * 2) + 1);
        final short cQZ = (short) ((z * 2) + 1);
        generated.put(new Point(x, z), region);

        for (short qX = (short) (cQX - QUAD_RADIUS); qX < cQX + QUAD_RADIUS; qX++) {
            for (short qZ = (short) (cQZ - QUAD_RADIUS); qZ < cQZ + QUAD_RADIUS; qZ++) {
                final short xO = (short) (qX / 2);
                final short zO = (short) (qZ / 2);
                // in current region
                if (xO == x && zO == z) {
                    if (!region.hasQuad(qX, qZ)) {
                        this.generateQuad(ctx, region, region, qX, qZ, x, z, partial);
                        region.setQuadGenerated(qX, qZ);
                    }
                    continue;
                }
                final Point pO = new Point(xO, zO);
                final RoadRegion rO = generated.computeIfAbsent(pO, p -> this.map.loadPartial((short) p.x, (short) p.z));
                if (rO.hasQuad(qX, qZ)) {
                    rO.copyQuadInto(region, qX, qZ);
                } else {
                    this.generateQuad(ctx, region, rO, qX, qZ, x, z, partial);
                    rO.setQuadGenerated(qX, qZ);
                }
                generated.put(pO, rO);
            }
        }
        // Previously kept all networks to avoid some overlap. Can safely remove them now.
        region.getData().removeIf(n -> !n.isInRegion(x, z));
        return generated;
    }

    private void generateQuad(
            PangaeaContext ctx, RoadRegion region, RoadRegion rO, short qX, short qZ, short pX, short pZ, boolean partial) {
        final int cXO = RoadRegion.quadToChunk(qX);
        final int cZO = RoadRegion.quadToChunk(qZ);
        ctx.enableDensityWrap(false);

        for (int cX = cXO; cX < cXO + RoadRegion.QUAD_CHUNK_LEN; cX++) {
            for (int cZ = cZO; cZ < cZO + RoadRegion.QUAD_CHUNK_LEN; cZ++) {
                final var generators = this.generators;
                for (int i = 0; i < generators.size(); i++) {
                    ctx.rand.setLargeFeatureSeed(ctx.seed + i, cX, cZ);
                    ctx.featureIndex.increment();
                    ctx.targetPos.at(cX << 4, cZ << 4);
                    final var n = generators.get(i).generateNetwork(ctx, region, cX, cZ, pX, pZ, partial);
                    if (n != null) {
                        final var o = n.origin().toPoint();
                        this.map.addNetwork(o, n);
                        region.addNetwork(o, n);
                        if (partial && region != rO) {
                            rO.addNetwork(o, n);
                        }
                    }
                }
            }
        }
        ctx.enableDensityWrap(true);
    }
}
