// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      Abdullah Younis
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================
import java.util.*;

public class MyAI extends Agent
{
    private int x;
    private int y;
    private int lastX;
    private int lastY;
    private int width;
    private int height;
    private int agentDir;
    private int numOfSafeUnvisited;
    private boolean arrow;
    private boolean wumpusIsLive;
    private boolean getGold;
    private double[][] isWumpus;
    private boolean[][] safe;
    private boolean[][] visited;
    private List<int[]> unsafeForWumpus;
    private Stack<int[]> route;  // to record the visited path

    // store next actions in a queue
    private LinkedList<Action> nextActions = new LinkedList<>();

    public MyAI ( )
    {
        x = 1;
        y = 1;
        lastX = 1;
        lastY = 1;
        width = 10;
        height = 10;
        agentDir = 0;
        numOfSafeUnvisited = 1;
        arrow = true;
        wumpusIsLive = true;
        getGold = false;
        isWumpus = new double[10][10];
        safe = new boolean[10][10];
        safe[1][1] = true;
        visited = new boolean[10][10];
        unsafeForWumpus = new ArrayList<>();
        route = new Stack<>();
    }

    public Action getAction
            (
                    boolean stench,
                    boolean breeze,
                    boolean glitter,
                    boolean bump,
                    boolean scream
            )
    {

        // ===============================================================
        // =			 Update Status & Do Next Actions
        // ===============================================================

        // if bump, take a step back
        if(bump) {
            if(x != lastX) {
                width = x - 1;
            }
            if(y != lastY) {
                height = y - 1;
            }
            nextActions = new LinkedList<>();
            x = lastX;
            y = lastY;
            numOfSafeUnvisited--;
            route.pop();
        }

        // mark (x,y) as visited
        if(!visited[x][y]) {
            numOfSafeUnvisited--;
        }
        visited[x][y] = true;
        if(route.empty() || route.peek()[0] != x || route.peek()[1] != y) {
            int[] crr = {x, y};
            route.add(crr);
        }

        // if kill the wumpus
        if(scream) {
            wumpusIsLive = false;
            for(int[] br: unsafeForWumpus) {
                safe[br[0]][br[1]] = true;
                numOfSafeUnvisited++;
            }
        }

        // do next actions
        if(nextActions.size() > 0) {
            actNext(nextActions.peek());
            return nextActions.poll();
        }

        // climb
        if(x == 1 && y == 1 && getGold) {
            return Action.CLIMB;
        }

        // grab the gold
        if(glitter) {
            getGold = true;
            return Action.GRAB;
        }

        // ===============================================================
        // =					  Explore Neighbors
        // ===============================================================

        // get coordinates of neighbors; the length of int[] is always 2
        int[][] neighbors = getNeighbors(x,y);

        // shoot if sensing a stench
        int numSafe = 0;
        if(!getGold && arrow && stench) {
            for(int[] nb: neighbors) {
                if(!safe[nb[0]][nb[1]]) {
                    isWumpus[nb[0]][nb[1]] += 0.5;
                    if(isWumpus[nb[0]][nb[1]] == 1.0 || (x == 1 && y == 1 && !breeze) || numSafe == neighbors.length - 1) { // if confirming where the wumpus is, or currently is at start point
                        shoot(nb);
                        return nextActions.poll();
                    }
                } else {
                    numSafe++;
                }
            }
        }

        // mark safe neighbors
        if(!breeze && (!wumpusIsLive || !stench)) {
            for(int[] nb: neighbors) {
                if(!safe[nb[0]][nb[1]]) {
                    numOfSafeUnvisited++;
                }
                safe[nb[0]][nb[1]] = true;
            }
        }

        // mark neighbors unsafe only due to wumpus
        if(!breeze && stench) {
            for(int[] nb: neighbors) {
                unsafeForWumpus.add(nb);
            }
        }

        // if getting gold, find a shortest path to the start point
        if(getGold) {
            toStart();
            stepBack();
            return nextActions.poll();
        }

        // choose a safe and unvisited neighbor to forward
        for(int[] nb: neighbors) {
            if(safe[nb[0]][nb[1]] && !visited[nb[0]][nb[1]]) {
                forward(nb);
                return nextActions.poll();
            }
        }

        // if there is no more to explore, climb at (1,1), otherwise take a step back
        if(x == 1 && y == 1) {
            return Action.CLIMB;
        } else {
            if(numOfSafeUnvisited == 0) {
                toStart();
            }
            stepBack();
            return nextActions.poll();
        }

    }

    // ===============================================================
    // =					 Action Functions
    // ===============================================================

    // take a step back
    private void stepBack(){
        route.pop();
        int[] next = route.pop();
        LinkedList<Action> acts = getActionsTo(next);
        nextActions.addAll(acts);
        nextActions.add(Action.FORWARD);
        x = next[0];
        y = next[1];
        actNext(nextActions.peek());
    }

    // find shortest path to the start point using BFS
    private void toStart() {
        boolean[][] vstd = new boolean[10][10];
        Queue<List<Integer>> queue = new LinkedList<>();
        HashMap<List<Integer>, List<Integer>> prev = new HashMap<>();

        vstd[x][y] = true;
        queue.add(new ArrayList<>(Arrays.asList(x,y)));

        while(!queue.isEmpty()) {
            List<Integer> u = queue.poll();
            int[][] nbs = getNeighbors(u.get(0), u.get(1));

            for(int[] brch: nbs) {
                if(safe[brch[0]][brch[1]] && !vstd[brch[0]][brch[1]]) {
                    vstd[brch[0]][brch[1]] = true;
                    queue.add(new ArrayList<>(Arrays.asList(brch[0],brch[1])));
                    prev.put(new ArrayList<>(Arrays.asList(brch[0],brch[1])), u);

                    if(brch[0] == 1 && brch[1] == 1) {
                        List<Integer> crrNode = new ArrayList<>(Arrays.asList(1,1));
                        route = new Stack<>();
                        route.add(new int[]{1,1});
                        while(crrNode.get(0) != x || crrNode.get(1) != y) {
                            crrNode = prev.get(crrNode);
                            route.add(new int[]{crrNode.get(0),crrNode.get(1)});
                        }
                        return;
                    }
                }
            }
        }

    }

    // forward a step
    private void forward(int[] nb) {
        LinkedList<Action> acts = getActionsTo(nb);
        nextActions.addAll(new LinkedList<>(acts));
        nextActions.add(Action.FORWARD);
        lastX = x;
        lastY = y;
        x = nb[0];
        y = nb[1];
        actNext(nextActions.peek());
    }

    // shoot
    private void shoot(int[] nb) {
        arrow = false;
        LinkedList<Action> acts = getActionsTo(nb);
        nextActions.addAll(new LinkedList<>(acts));
        nextActions.add(Action.SHOOT);
        actNext(nextActions.peek());
    }

    // ===============================================================
    // =					 Action Helpers
    // ===============================================================

    // get neighbors of (x,y)
    private int[][] getNeighbors(int x, int y) {
        int left = x - 1;
        int right = x + 1;
        int up = y + 1;
        int down = y - 1;

        int n = 0;
        if(right <= width) {
            n++;
        }
        if(left >= 1) {
            n++;
        }
        if(up <= height) {
            n++;
        }
        if(down >= 1) {
            n++;
        }

        int[][] nb = new int[n][2];

        int i = 0;
        if(right <= width) {
            nb[i][0] = x + 1;
            nb[i][1] = y;
            i++;
        }
        if(left >= 1) {
            nb[i][0] = x - 1;
            nb[i][1] = y;
            i++;
        }
        if(up <= height) {
            nb[i][0] = x;
            nb[i][1] = y + 1;
            i++;
        }
        if(down >= 1) {
            nb[i][0] = x;
            nb[i][1] = y - 1;
            i++;
        }
        return nb;
    }

    // get actions to next steps
    private LinkedList<Action> getActionsTo(int[] next) {
        LinkedList<Action> res = new LinkedList<>();

        int nextDir = agentDir;
        // left
        if(x == next[0] + 1) {
            nextDir = 2;
        }
        // right
        if(x == next[0] - 1) {
            nextDir = 0;
        }
        // up
        if(y == next[1] - 1) {
            nextDir = 3;
        }
        // down
        if(y == next[1] + 1) {
            nextDir = 1;
        }

        int gap = (nextDir - agentDir + 4) % 4;
        if(gap == 0) {
            return res;
        } else if(gap == 1) {
            res.add(Action.TURN_RIGHT);
        } else if(gap == 2) {
            res.add(Action.TURN_RIGHT);
            res.add(Action.TURN_RIGHT);
        } else {
            res.add(Action.TURN_LEFT);
        }

        return res;
    }

    // after do one action, update agentDir
    private void actNext(Action act) {
        if (act == Action.TURN_LEFT) {
            agentDir = (agentDir + 3) % 4;
        } else if (act == Action.TURN_RIGHT) {
            agentDir = (agentDir + 1) % 4;
        }
    }

}