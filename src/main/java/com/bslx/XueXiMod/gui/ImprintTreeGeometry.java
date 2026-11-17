package com.bslx.XueXiMod.gui;

import java.util.*;

/** Native-size tiles on authored constellation lanes. Only LearningGraph supplies prerequisites. */
public final class ImprintTreeGeometry {
    public static final double MIN_ZOOM = 0.75, MAX_ZOOM = 2.0;
    private static final int GRID = 4;
    // Permanent growth corridors. Martial/water/rule branches cannot occupy wood/earth/metal space.
    public static final double[] SECTOR_START = {215, 265, 345, 75, 45, 165, 115};
    public static final double[] SECTOR_END = {265, 345, 405, 115, 75, 215, 165};
    public static final int[][] ANCHORS = {
            {-42, -66}, {42, -66}, {64, -20}, {-24, 68}, {24, 66}, {-64, -26}, {-56, 36}
    };
    private ImprintTreeGeometry() { }
    public static final class Spec {
        public final String id;
        public final int group, nameWidth, lineHeight;
        public final List<List<String>> parents;
        public Spec(String id, int group, int nameWidth, int lineHeight, List<List<String>> parents) {
            this.id = id; this.group = Math.floorMod(group, 7);
            this.nameWidth = Math.max(1, nameWidth); this.lineHeight = Math.max(1, lineHeight);
            this.parents = parents == null ? List.of() : parents;
        }
    }
    public static final class Route {
        public final int parent, child, group;
        public final double[] x, y;
        public final boolean trunk;
        Route(int parent, int child, int group, boolean trunk, List<Point> points) {
            this.parent = parent; this.child = child; this.group = group; this.trunk = trunk;
            x = new double[points.size()]; y = new double[points.size()];
            for (int i = 0; i < points.size(); i++) { x[i] = points.get(i).x; y[i] = points.get(i).y; }
        }
    }
    public static final class Layout {
        public final double[] x, y;
        public final List<Route> routes;
        public final int rawEdges, uniqueEdges;
        public final int[] depth;
        Layout(double[] x, double[] y, List<Route> routes, int raw, int unique, int[] depth) {
            this.x = x; this.y = y; this.routes = List.copyOf(routes);
            rawEdges = raw; uniqueEdges = unique; this.depth = depth;
        }
    }
    private record Point(double x, double y) { }
    private record Edge(int from, int to) { }

    public static Layout layout(List<Spec> specs) {
        int n = specs.size(), raw = 0;
        Map<String, Integer> ids = new HashMap<>();
        for (int i = 0; i < n; i++) ids.put(specs.get(i).id, i);
        List<Edge> edges = new ArrayList<>();
        for (int child = 0; child < n; child++) for (List<String> alternatives : specs.get(child).parents)
            for (String id : alternatives) {
                Integer parent = ids.get(id);
                if (parent == null || parent == child) continue;
                raw++;
                Edge edge = new Edge(parent, child);
                if (!edges.contains(edge)) edges.add(edge);
            }
        int[] depth = new int[n];
        for (int pass = 0; pass < n; pass++) {
            boolean changed = false;
            for (Edge e : edges) if (depth[e.to] <= depth[e.from]) { depth[e.to] = depth[e.from] + 1; changed = true; }
            if (!changed) break;
            if (pass == n - 1) throw new IllegalArgumentException("Cyclic imprint learning graph");
        }
        Map<String, Point> guides = guides();
        // Linear progressions own straight radial lanes, independent of the other root clusters.
        ray(specs,ids,edges,guides,"rule_dual_release",208,80,155);
        chain(specs,ids,edges,guides,"rule_double_cast",-240,280,-72,72);
        double[] x = new double[n], y = new double[n]; boolean[] placed = new boolean[n];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        order.sort(Comparator.comparingInt((Integer i) -> depth[i]).thenComparing(i -> specs.get(i).id));
        for (int i : order) {
            Point guide = guides.get(specs.get(i).id);
            if (guide == null) guide = fallback(specs, edges, i, x, y, placed, depth[i]);
            double requiredRadius = 0;
            for (Edge e : edges) if (e.to == i)
                requiredRadius = Math.max(requiredRadius, Math.hypot(x[e.from], y[e.from]) + 1);
            double radius = Math.hypot(guide.x, guide.y);
            if (radius < requiredRadius) guide = new Point(snap(guide.x * requiredRadius / radius), snap(guide.y * requiredRadius / radius));
            Point p = guide;
            for (int attempt = 0; attempt < 200 && occupied(specs, i, p, x, y, placed); attempt++) {
                double a = Math.atan2(guide.y, guide.x), radial = 8 * (attempt / 5 + 1), side = (attempt % 5 - 2) * 8;
                p = new Point(snap(guide.x + Math.cos(a) * radial - Math.sin(a) * side),
                        snap(guide.y + Math.sin(a) * radial + Math.cos(a) * side));
            }
            if (occupied(specs, i, p, x, y, placed)) {
                double angle = Math.toRadians((SECTOR_START[specs.get(i).group] + SECTOR_END[specs.get(i).group]) / 2);
                double fallbackRadius = Math.max(requiredRadius, Math.hypot(p.x, p.y));
                do {
                    fallbackRadius += 80;
                    p = new Point(snap(Math.cos(angle) * fallbackRadius), snap(Math.sin(angle) * fallbackRadius));
                } while (occupied(specs, i, p, x, y, placed));
            }
            x[i] = p.x; y[i] = p.y; placed[i] = true;
        }
        Router router = new Router(specs, x, y);
        edges.sort(Comparator.comparingDouble(e -> Math.hypot(x[e.from] - x[e.to], y[e.from] - y[e.to])));
        for (Edge e : edges) router.connect(e.from, e.to, false);
        router.roots(edges);
        return new Layout(x, y, router.routes, raw, edges.size(), depth);
    }
    private static void chain(List<Spec> specs, Map<String, Integer> ids, List<Edge> edges,
                              Map<String, Point> guides, String root, double x, double y, double dx, double dy) {
        Integer node = ids.get(root); Set<Integer> seen = new HashSet<>();
        while (node != null && seen.add(node)) {
            guides.put(specs.get(node).id, new Point(x, y));
            int parent = node;
            List<Edge> children = edges.stream().filter(e -> e.from == parent).toList();
            node = children.size() == 1 ? children.get(0).to : null;
            x += dx; y += dy;
        }
    }
    private static void ray(List<Spec> specs, Map<String,Integer> ids, List<Edge> edges,
                            Map<String,Point> guides,String root,double radius,double step,double degrees) {
        Integer node=ids.get(root);Set<Integer> seen=new HashSet<>();double a=Math.toRadians(degrees);
        while(node!=null && seen.add(node)) {
            guides.put(specs.get(node).id,new Point(snap(Math.cos(a)*radius),snap(Math.sin(a)*radius)));
            int parent=node;List<Edge> children=edges.stream().filter(e->e.from==parent).toList();
            node=children.size()==1?children.get(0).to:null;radius+=step;
        }
    }
    private static boolean inSector(int group, Point p) {
        double a=Math.toDegrees(Math.atan2(p.y,p.x));
        while(a<SECTOR_START[group]) a+=360;
        return a<=SECTOR_END[group]+.0001;
    }
    /** Family guides are UI composition only, never gameplay or inferred prerequisites. */
    private static Map<String, Point> guides() {
        Map<String, Point> g = new HashMap<>();
        put(g, -104, -192, "vine_mark");
        put(g, -152, -272, "parasite_seed");
        put(g, -200, -32, "phantom_sword");
        put(g, -280, -32, "sword_array");
        put(g, -360, -32, "phantom_sword_surge");
        put(g, -440, -32, "sword_return_all");
        put(g, -280, -104, "sword_come");
        put(g, -280, 32, "sword_flight");
        put(g, -360, -104, "space_cut");
        put(g, -360, -176, "sword_thrust");
        put(g, -440, -176, "sword_horizontal_slash");
        put(g, -440, -248, "sword_vertical_slash");
        put(g, -520, -216, "sword_oblique_slash");
        put(g, -520, -288, "sword_guard");
        put(g, -360, 40, "movement_dash");
        put(g, -440, 40, "movement_leap");
        put(g, -520, 72, "movement_spin");
        put(g, -520, 0, "clone_body");
        put(g, -520, -72, "rule_cooperative");
        put(g, -440, -104, "magic_arcane_missile");
        put(g, -520, -144, "magic_create_water");
        put(g, 96, -176, "fire_seed_sow");
        put(g, 176, -176, "fire_kindle");
        put(g, 296, -280, "fire_heat_up");
        put(g, 296, -360, "fire_cool");
        put(g, 72, -280, "fire_pick_lamp");
        put(g, 136, -280, "fire_detonate");
        put(g, 248, -216, "fire_scorch_field");
        put(g, 72, -360, "fire_liaoli");
        put(g, 200, -360, "fire_burn");
        put(g, 376, -280, "fire_liaoyuan");
        put(g, 336, -440, "fire_fentian");
        put(g, 160, -96, "fire_spark");
        put(g, 240, -128, "fire_orb");
        put(g, 320, -160, "fire_burst");
        put(g, 400, -192, "heat_beam");
        put(g, 456, -264, "fire_meteorite");
        put(g, 456, -336, "fire_shock_ring");
        put(g, 16, -176, "magic_ignite");
        put(g, 16, -248, "fire_flame_trail");
        put(g, 16, -320, "fire_upwell");
        put(g, 16, -392, "fire_nether_gloom");
        put(g, 176, 16, "tide_mark");
        put(g, 192, 88, "water_wave");
        put(g, 272, 16, "water_prison");
        put(g, 288, 88, "water_disperse");
        put(g, 264, 160, "water_vortex");
        put(g, 320, 232, "water_bubble_column");
        put(g, 448, 144, "water_mist");
        put(g, 448, 240, "water_deep_pressure");
        put(g, 320, -40, "water_weeping_arc");
        put(g, 400, -64, "water_phantom");
        put(g, 400, 16, "water_blade");
        put(g, 400, 96, "tide_sever");
        put(g, 480, 16, "water_curtain");
        put(g, 480, 96, "water_ring");
        put(g, 568, 56, "water_yielding_ribbon");
        put(g, -24, 192, "stone_hammer");
        put(g, -56, 272, "sinking_edge");
        put(g, 32, 288, "earth_break_ground");
        put(g, 112, 208, "keen_mark");
        put(g, 160, 288, "metal_needle");
        put(g, -264, 432, "rule_cast_anchor");
        put(g, -296, 512, "rule_surface_anchor");
        put(g, -376, 560, "trajectory_3d_interleave");
        put(g, -216, 360, "universal_lock_target");
        put(g, -456, 328, "trajectory_parallel");
        put(g, -536, 328, "trajectory_drop");
        put(g, -456, 408, "trajectory_tracking");
        put(g, -536, 408, "trajectory_storm");
        put(g, -160, 176, "rule_gather_energy");
        put(g, -240, 192, "rule_stagger");
        put(g, -320, 224, "test_sustained");
        put(g, -384, 288, "rule_hit_continue");
        put(g, -320, 288, "rule_domain_bind");
        return g;
    }
    private static void put(Map<String, Point> g, double x, double y, String id) { g.put(id, new Point(x, y)); }
    private static double snap(double value) { return Math.rint(value / GRID) * GRID; }
    private static Point fallback(List<Spec> specs, List<Edge> edges, int i, double[] x, double[] y, boolean[] placed, int depth) {
        double px = 0, py = 0; int parents = 0;
        for (Edge e : edges) if (e.to == i && placed[e.from]) { px += x[e.from]; py += y[e.from]; parents++; }
        double[] angles = {-120, -55, 15, 95, 60, 190, 140};
        int group = specs.get(i).group;
        double degrees = parents > 0 ? Math.toDegrees(Math.atan2(py, px)) : angles[group];
        while (degrees < SECTOR_START[group]) degrees += 360;
        if (degrees > SECTOR_END[group]) degrees = (SECTOR_START[group] + SECTOR_END[group]) / 2;
        degrees = Math.max(SECTOR_START[group] + 8, Math.min(SECTOR_END[group] - 8, degrees));
        double a = Math.toRadians(degrees);
        double r = parents > 0 ? Math.hypot(px / parents, py / parents) + 80 : 192 + depth * 76;
        return new Point(snap(Math.cos(a) * r), snap(Math.sin(a) * r));
    }
    public static double halfWidth(Spec s) { return Math.max(17, s.nameWidth / 2.0 + 4); }
    public static double top(Spec s) { return 18; }
    public static double bottom(Spec s) { return 16 + s.lineHeight; }
    public static boolean separated(Spec a, double ax, double ay, Spec b, double bx, double by, double zoom) {
        return Math.abs(ax - bx) * zoom >= halfWidth(a) + halfWidth(b) + 4
                || (by - ay) * zoom >= bottom(a) + top(b) + 4 || (ay - by) * zoom >= bottom(b) + top(a) + 4;
    }
    private static boolean occupied(List<Spec> specs, int i, Point p, double[] x, double[] y, boolean[] placed) {
        Spec spec = specs.get(i);
        if (altarHit(spec, p.x, p.y, MIN_ZOOM)) return true;
        double w=halfWidth(spec)/MIN_ZOOM;
        for(double dx:new double[]{-w,w}) for(double dy:new double[]{-top(spec)/MIN_ZOOM,bottom(spec)/MIN_ZOOM})
            if(!inSector(spec.group,new Point(p.x+dx,p.y+dy))) return true;
        for (int j = 0; j < specs.size(); j++) if (placed[j] && !separated(specs.get(i), p.x, p.y, specs.get(j), x[j], y[j], MIN_ZOOM)) return true;
        return false;
    }
    private static boolean altarHit(Spec s, double x, double y, double zoom) {
        return x * zoom + halfWidth(s) > -98 && x * zoom - halfWidth(s) < 98
                && y * zoom + bottom(s) > -100 && y * zoom - top(s) < 100;
    }

    private static final class Router {
        final List<Spec> specs; final double[] x, y;
        final List<Route> routes = new ArrayList<>();
        Router(List<Spec> specs, double[] x, double[] y) { this.specs = specs; this.x = x; this.y = y; }
        Point activeJoint, activeStart;
        int activeGroup;
        boolean decorative;
        void connect(int parent, int child, boolean trunk) {
            Point a = new Point(x[parent], y[parent]), b = new Point(x[child], y[child]);
            decorative = trunk; activeStart = a; activeGroup = specs.get(child).group;
            Point joint = alternativeJoint(parent, child); activeJoint = joint;
            List<Point> points;
            if (joint == null) points = path(a, b, parent, child);
            else {
                points = new ArrayList<>(path(a, joint, parent, child));
                List<Point> tail = path(joint, b, parent, child);
                points.addAll(tail.subList(1, tail.size()));
            }
            activeJoint = null;
            routes.add(new Route(parent, child, specs.get(child).group, trunk, points));
        }
        Set<String> parentSet(int child) {
            Set<String> result = new TreeSet<>();
            for (List<String> group : specs.get(child).parents) result.addAll(group);
            return result;
        }
        Point alternativeJoint(int parent, int child) {
            Set<String> chosen = parentSet(child);
            if (chosen.size() < 2 || specs.get(child).group == 5) return null;
            double sx=0,sy=0,maxX=-10000,minY=10000,maxY=-10000;int count=0;
            for (int i=0;i<specs.size();i++) if (chosen.contains(specs.get(i).id)) {
                sx+=x[i];sy+=y[i];count++;maxX=Math.max(maxX,x[i]);minY=Math.min(minY,y[i]);maxY=Math.max(maxY,y[i]);
            }
            int g=specs.get(child).group;
            if (chosen.size()>2) return new Point(snap(sx/count),snap(sy/count));
            if (g==1 && Math.abs(maxY-minY)<1 || g==6)
                return new Point(snap(sx/count),snap(sy/count+(g==1?-40:40)));
            return new Point(snap(maxX+40),snap(sy/count));
        }
        List<Point> path(Point a, Point b, int parent, int child) {
            List<List<Point>> candidates = new ArrayList<>();
            candidates.add(List.of(a, b)); candidates.add(List.of(a, new Point(a.x, b.y), b)); candidates.add(List.of(a, new Point(b.x, a.y), b));
            double mx = snap((a.x + b.x) / 2), my = snap((a.y + b.y) / 2);
            for (int offset = 0; offset <= 144; offset += 16) for (int sign : new int[]{-1, 1}) {
                candidates.add(List.of(a, new Point(mx + sign * offset, a.y), new Point(mx + sign * offset, b.y), b));
                candidates.add(List.of(a, new Point(a.x, my + sign * offset), new Point(b.x, my + sign * offset), b));
            }
            candidates.sort(Comparator.comparingDouble(ImprintTreeGeometry::length));
            for (List<Point> candidate : candidates) if (clear(candidate, parent, child)) return simplify(candidate);
            return search(a, b, parent, child);
        }
        boolean clear(List<Point> path, int parent, int child) {
            for (int i = 1; i < path.size(); i++) if (blocked(path.get(i - 1), path.get(i), parent, child)) return false;
            return true;
        }
        boolean blocked(Point a, Point b, int parent, int child) {
            if (!inSector(activeGroup,a) || !inSector(activeGroup,b)) return true;
            if (clip(a.x, a.y, b.x, b.y, -98 / MIN_ZOOM, -100 / MIN_ZOOM, 98 / MIN_ZOOM, 100 / MIN_ZOOM) != null) return true;
            for (int i = 0; i < specs.size(); i++) {
                if (i == parent || i == child) continue;
                Spec s = specs.get(i); double w = (halfWidth(s) + 2) / MIN_ZOOM;
                if (clip(a.x, a.y, b.x, b.y, x[i] - w, y[i] - (top(s) + 2) / MIN_ZOOM,
                        x[i] + w, y[i] + (bottom(s) + 2) / MIN_ZOOM) != null) return true;
            }
            for (Route r : routes) for (int j = 1; j < r.x.length; j++) {
                Point c = new Point(r.x[j - 1], r.y[j - 1]), d = new Point(r.x[j], r.y[j]);
                if (!intersects(a, b, c, d)) continue;
                if (sharesStem(a, b, c, d, parent, child, r)) continue;
                if (activeJoint != null && atJoint(a,b,c,d,activeJoint)) continue;
                if (activeJoint != null && r.child == child && j == r.x.length-1
                        && (near(a,activeJoint)||near(b,activeJoint)) && onSegment(c,d,activeJoint)) continue;
                if (decorative && r.group == activeGroup && activeStart != null && atJoint(a,b,c,d,activeStart)) continue;
                return true;
            }
            return false;
        }
        boolean atJoint(Point a, Point b, Point c, Point d, Point p) {
            if ((near(a,p)||near(b,p)) && (near(c,p)||near(d,p))) return true;
            return (near(c,p)||near(d,p)) && Math.abs(cross(a,b,c))<.001 && Math.abs(cross(a,b,d))<.001;
        }
        boolean sharesStem(Point a, Point b, Point c, Point d, int parent, int child, Route r) {
            for (int i : new int[]{parent, child}) {
                if (i < 0 || r.parent != i && r.child != i) continue;
                Point p = new Point(x[i], y[i]);
                if ((near(a, p) || near(b, p)) && (near(c, p) || near(d, p))) return true;
                if (Math.abs(cross(a, b, c)) < .001 && Math.abs(cross(a, b, d)) < .001
                        && Math.min(distance(a, p), distance(b, p)) < 48) return true;
            }
            return false;
        }
        List<Point> search(Point a, Point b, int parent, int child) {
            int margin = 128;
            int left = (int) snap(Math.min(a.x, b.x) - margin), top = (int) snap(Math.min(a.y, b.y) - margin);
            int w = (int) (Math.abs(a.x - b.x) + margin * 2) / GRID + 1, h = (int) (Math.abs(a.y - b.y) + margin * 2) / GRID + 1;
            int start = ((int) (a.y - top) / GRID * w + (int) (a.x - left) / GRID) * 4;
            int target = (int) (b.y - top) / GRID * w + (int) (b.x - left) / GRID;
            double[] cost = new double[w * h * 4]; Arrays.fill(cost, Double.POSITIVE_INFINITY);
            int[] previous = new int[cost.length]; Arrays.fill(previous, -1);
            record Visit(int key, double cost, double score) { }
            PriorityQueue<Visit> queue = new PriorityQueue<>(Comparator.comparingDouble(Visit::score));
            for (int dir = 0; dir < 4; dir++) { cost[start + dir] = 0; queue.add(new Visit(start + dir, 0, 0)); }
            int[] dx = {1, 0, -1, 0}, dy = {0, 1, 0, -1}; int end = -1;
            while (!queue.isEmpty()) {
                Visit visit = queue.remove(); int key = visit.key;
                if (visit.cost != cost[key]) continue;
                int cell = key / 4, cx = cell % w, cy = cell / w;
                if (cell == target) { end = key; break; }
                Point p = new Point(left + cx * GRID, top + cy * GRID);
                for (int dir = 0; dir < 4; dir++) {
                    int nx = cx + dx[dir], ny = cy + dy[dir];
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                    Point q = new Point(left + nx * GRID, top + ny * GRID);
                    if (blocked(p, q, parent, child)) continue;
                    int next = (ny * w + nx) * 4 + dir;
                    double value = visit.cost + GRID + (dir == key % 4 ? 0 : 18);
                    if (value >= cost[next]) continue;
                    cost[next] = value; previous[next] = key;
                    queue.add(new Visit(next, value, value + Math.abs(q.x - b.x) + Math.abs(q.y - b.y)));
                }
            }
            if (end < 0) throw new IllegalStateException("No collision-free imprint route: "
                    + (parent<0?"stem":specs.get(parent).id)+" -> "+specs.get(child).id);
            List<Point> result = new ArrayList<>();
            for (int key = end; key >= 0; key = previous[key]) {
                int cell = key / 4; result.add(new Point(left + cell % w * GRID, top + cell / w * GRID));
            }
            Collections.reverse(result); return simplify(result);
        }
        void roots(List<Edge> edges) {
            boolean[] hasParent = new boolean[specs.size()]; for (Edge e : edges) hasParent[e.to] = true;
            int[] component = new int[specs.size()];
            for (int i=0;i<component.length;i++) component[i]=i;
            for (int pass=0;pass<specs.size();pass++) for (Edge e:edges) {
                int c=Math.min(component[e.from],component[e.to]);component[e.from]=c;component[e.to]=c;
            }
            for (int group = 0; group < 7; group++) {
                List<Integer> roots = new ArrayList<>();
                for (int i = 0; i < specs.size(); i++) if (specs.get(i).group == group && !hasParent[i]) roots.add(i);
                roots.sort(Comparator.comparingDouble(i -> Math.hypot(x[i], y[i])));
                if (roots.isEmpty()) continue;
                int first = roots.remove(0);
                Point a = new Point(snap(ANCHORS[group][0] / MIN_ZOOM), snap(ANCHORS[group][1] / MIN_ZOOM));
                routes.add(new Route(-1, first, group, true, List.of(a, new Point(x[first], y[first]))));
                Set<Integer> attached = new HashSet<>(); attached.add(component[first]);
                roots.removeIf(i -> attached.contains(component[i]));
                while (!roots.isEmpty()) {
                    record Attachment(int root, int parent, Point point, double distance) { }
                    List<Attachment> choices = new ArrayList<>();
                    for (int root : roots) for (Route r : routes) {
                        if (r.group != group || !attached.contains(component[r.child])) continue;
                        for (int v = r.parent == -1 ? 1 : 0; v < r.x.length; v++) {
                            Point point=new Point(r.x[v],r.y[v]);
                            int parent=v==0?r.parent:v==r.x.length-1?r.child:-2;
                            choices.add(new Attachment(root,parent,point,Math.hypot(x[root]-point.x,y[root]-point.y)));
                        }
                    }
                    choices.sort(Comparator.comparingDouble(Attachment::distance));
                    int bestRoot=-1;
                    for(Attachment choice:choices) {
                        decorative=true;activeStart=choice.point;activeGroup=group;activeJoint=null;
                        try {
                            List<Point> points=path(choice.point,new Point(x[choice.root],y[choice.root]),choice.parent,choice.root);
                            routes.add(new Route(choice.parent,choice.root,group,true,points));
                            bestRoot=choice.root;break;
                        } catch(IllegalStateException blocked) {
                            // A dependency cycle can enclose the nearest stem. Try its outer junction.
                        }
                    }
                    if(bestRoot<0) throw new IllegalStateException("No open root stem for sector "+group);
                    attached.add(component[bestRoot]);
                    roots.removeIf(i -> attached.contains(component[i]));
                }
            }
        }
    }
    private static boolean near(Point a, Point b) { return distance(a, b) < .01; }
    private static double distance(Point a, Point b) { return Math.hypot(a.x - b.x, a.y - b.y); }
    private static double cross(Point a, Point b, Point c) { return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x); }
    private static boolean onSegment(Point a, Point b, Point p) {
        return Math.abs(cross(a,b,p))<.001 && p.x>=Math.min(a.x,b.x)-.001 && p.x<=Math.max(a.x,b.x)+.001
                && p.y>=Math.min(a.y,b.y)-.001 && p.y<=Math.max(a.y,b.y)+.001;
    }
    private static boolean intersects(Point a, Point b, Point c, Point d) {
        if (Math.max(a.x, b.x) + .001 < Math.min(c.x, d.x) || Math.max(c.x, d.x) + .001 < Math.min(a.x, b.x)
                || Math.max(a.y, b.y) + .001 < Math.min(c.y, d.y) || Math.max(c.y, d.y) + .001 < Math.min(a.y, b.y)) return false;
        return cross(a, b, c) * cross(a, b, d) <= .001 && cross(c, d, a) * cross(c, d, b) <= .001;
    }
    private static List<Point> simplify(List<Point> input) {
        List<Point> result = new ArrayList<>();
        for (Point p : input) {
            if (!result.isEmpty() && near(result.get(result.size() - 1), p)) continue;
            while (result.size() > 1 && Math.abs(cross(result.get(result.size() - 2), result.get(result.size() - 1), p)) < .001) result.remove(result.size() - 1);
            result.add(p);
        }
        return result;
    }
    private static double length(List<Point> points) {
        double length = (points.size() - 2) * 10;
        for (int i = 1; i < points.size(); i++) length += distance(points.get(i - 1), points.get(i));
        return length;
    }
    public static double[] clip(double x1, double y1, double x2, double y2, double left, double top, double right, double bottom) {
        double dx = x2 - x1, dy = y2 - y1, start = 0, end = 1;
        double[] p = {-dx, dx, -dy, dy}, q = {x1 - left, right - x1, y1 - top, bottom - y1};
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i]) < 1e-8) { if (q[i] < 0) return null; }
            else {
                double t = q[i] / p[i];
                if (p[i] < 0) start = Math.max(start, t); else end = Math.min(end, t);
                if (start > end) return null;
            }
        }
        return new double[]{start, end};
    }
    public static String audit(List<Spec> specs, Layout layout) {
        int[] groups = new int[7]; double radius = 0; int overlaps = 0, altar = 0, hits = 0, crossings = 0;
        for (int i = 0; i < specs.size(); i++) {
            groups[specs.get(i).group]++; radius = Math.max(radius, Math.hypot(layout.x[i], layout.y[i]));
            if (altarHit(specs.get(i), layout.x[i], layout.y[i], MIN_ZOOM)) altar++;
            for (int j = 0; j < i; j++) if (!separated(specs.get(i), layout.x[i], layout.y[i], specs.get(j), layout.x[j], layout.y[j], MIN_ZOOM)) overlaps++;
        }
        for (int ri = 0; ri < layout.routes.size(); ri++) {
            Route r = layout.routes.get(ri);
            for (int s = 1; s < r.x.length; s++) {
                Point a = new Point(r.x[s - 1], r.y[s - 1]), b = new Point(r.x[s], r.y[s]);
                for (int i = 0; i < specs.size(); i++) {
                    if (i == r.parent || i == r.child) continue;
                    Spec spec = specs.get(i); double w = halfWidth(spec) / MIN_ZOOM;
                    if (clip(a.x, a.y, b.x, b.y, layout.x[i] - w, layout.y[i] - top(spec) / MIN_ZOOM,
                            layout.x[i] + w, layout.y[i] + bottom(spec) / MIN_ZOOM) != null) hits++;
                }
                for (int other = 0; other < ri; other++) {
                    Route t = layout.routes.get(other);
                    for (int v = 1; v < t.x.length; v++) {
                        Point c = new Point(t.x[v - 1], t.y[v - 1]), d = new Point(t.x[v], t.y[v]);
                        if (cross(a, b, c) * cross(a, b, d) < -.001 && cross(c, d, a) * cross(c, d, b) < -.001) crossings++;
                    }
                }
            }
        }
        return "nodes=" + specs.size() + " groups=" + Arrays.toString(groups) + " maxRadius=" + radius
                + " rawEdges=" + layout.rawEdges + " uniqueEdges=" + layout.uniqueEdges + " routes=" + layout.routes.size()
                + " minZoom=" + MIN_ZOOM + " maxZoom=" + MAX_ZOOM + " overlaps=" + overlaps + " altar=" + altar
                + " nodeHits=" + hits + " crossings=" + crossings;
    }
}

