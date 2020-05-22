
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class LocalSearch {
    int nodes;
    int endOfDuty = 18*60*60; // 6pm in second======
    int startOfDuty = 6*60*60; // 6am in second======
    HashMap<Integer, Location> locationHashMap;
    HashMap<Integer, Window> windowHashMap;
    HashMap<Integer, Long> timeOfPositions;
    List<Integer> nodeList;
    Integer timeStamp[];
    Location headOffice = new Location(23.7938636,90.4053257); // AR Tower location

    public LocalSearch() {
        locationHashMap = new HashMap();
        windowHashMap = new HashMap();
        timeOfPositions = new HashMap();
        nodeList = new ArrayList();
        takeInput();
    }

    private void takeInput() {
        try(RandomAccessFile input = new RandomAccessFile("input.txt", "r")) {
            String line;
            nodes = Integer.parseInt(input.readLine());

            for(int i=1; i<=nodes; i++){
                line = input.readLine();
                String tokens[] = line.split("\\ ");
                nodeList.add(i);
                Double longitude = Double.parseDouble(tokens[0]);
                Double latitude = Double.parseDouble(tokens[1]);
                locationHashMap.put(i, new Location(longitude, latitude));

                int startTime = Integer.parseInt(tokens[2]);
                int endTime = Integer.parseInt(tokens[3]);
                windowHashMap.put(i, new Window(startTime, endTime));
            }

//            Long penalty = getPenaltyDP(0, nodeList,
//                    startOfDuty+getTravelTime(headOffice, locationHashMap.get(nodeList.get(0))));
//            System.out.println(penalty);
//            printTimeOfPosition(timeOfPositions);
            //printHashMap(locationHashMap);
            //printWindowHashMap(windowHashMap);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printLocationHashMap(HashMap<Integer, Location> locationHashMap){
        System.out.println(locationHashMap.toString());
    }

    public void printWindowHashMap(HashMap<Integer, Window> windowHashMap){
        System.out.println(windowHashMap.toString());
    }

    public void printTimeOfPosition(HashMap<Integer, Long> timeOfPositions){
        timeOfPositions.forEach((node, time) -> System.out.println(node + ": " + getTimeIn24(time)));
    }

    public double acceptanceProbability(Long currentPenalty,
                                        Long previousPenalty,
                                        double currentTemperature) {
        if (currentPenalty <= previousPenalty)
            return 1;

        double delta = currentPenalty - previousPenalty;
        return Math.exp(-delta / currentTemperature);
    }

    public void startSearch(){
        final double initialTemperature = 100;
        final double coolingRate = 1E-4;
        timeStamp = new Integer[nodes+10];

        //Adding HeadOffice location in the locationHashMap===
        locationHashMap.put(0, headOffice);
        locationHashMap.put(nodes+1, headOffice);

        //Adding HeadOffice window in the windowHashMap======
        windowHashMap.put(0, new Window(startOfDuty, endOfDuty));
        windowHashMap.put(nodes+1, new Window(startOfDuty, endOfDuty));

        double currentTemperature = initialTemperature;

        List<Integer> currentPath = nodeList;
        final List<Integer> bestPath = new ArrayList();

        currentPath.forEach(node -> bestPath.add(node));

        //adding headOffice to the front and back======
        currentPath.add(0, 0); currentPath.add(nodeList.size());
        //====================================================================

        Long currentPenalty = getPenaltyDP(0, currentPath, (long)startOfDuty);

        // removing headOffice from the path===
        currentPath.remove(0); currentPath.remove(currentPath.size()-1);
        //====================================================================

        Long previousPenalty;
        Long bestPenalty = currentPenalty;

        int iteration = 1;

        while(currentTemperature > 0 && bestPenalty > 0){
            previousPenalty = currentPenalty;
            List<Integer> nextRandomPath = new ArrayList();

            currentPath.forEach(node -> nextRandomPath.add(node));
            Collections.shuffle(nextRandomPath); // getting random path==========

            //adding headOffice to the front and back======
            nextRandomPath.add(0, 0); nextRandomPath.add(nodeList.size());
            //=======================================================================
            currentPenalty = getPenaltyDP(0, nextRandomPath, (long)startOfDuty);

            // removing headOffice from the path===
            nextRandomPath.remove(0); nextRandomPath.remove(nextRandomPath.size()-1);
            //=======================================================================

            double random = Math.random();
            if(random < acceptanceProbability(currentPenalty, previousPenalty, currentTemperature)) {
                //changes are acceptable===
                if (currentPenalty < bestPenalty) {
                    bestPenalty = currentPenalty;
                    bestPath.clear();
                    nextRandomPath.forEach(node -> bestPath.add(node));

                    //System.out.println("Iteration: " + iteration + ": " + bestPath + " Penalty: " + bestPenalty);
                }
            }
            currentTemperature -= coolingRate;
            iteration++;
        }

        setTimeOfPositions(bestPath);
        System.out.println("BestPath: " + bestPath);
        System.out.println(headOffice.toString() + " Time: " + getTimeIn24((long)startOfDuty));
        bestPath.forEach(node -> System.out.println(locationHashMap.get(node).toString() + " Time: " + getTimeIn24(timeOfPositions.get(node))));
        int lastNode = bestPath.get(bestPath.size()-1);
        Long travelTime = getTravelTime(locationHashMap.get(lastNode), headOffice);
        System.out.println(headOffice.toString() + " Time: " +
                getTimeIn24(timeOfPositions.get(lastNode)+travelTime));

        System.out.println("Total Penalty: " + bestPenalty);
    }

    public Long getPenaltyDP(int pos, List<Integer> nodeList, Long currentTime){
        // I am using dp because I may wait for a node.window.startTime() or I may get penalty
        // in which way is best I will take it
        // so I am choosing the best by waiting and getting penalty

        // The first and last element of the nodeList is the HeadOffice======

        if(pos >= nodeList.size()-2) return 0l;

        // If we can't reach next terminal in duty time===
        if(currentTime+getTravelTime(locationHashMap.get(nodeList.get(pos)),
                locationHashMap.get(nodeList.get(pos+1)))+120 > endOfDuty){
            return (nodeList.size()-pos-2) * 10000l;
        }

        Window window = windowHashMap.get(nodeList.get(pos+1));
        Long p=Long.MAX_VALUE, q=Long.MAX_VALUE;
        Long travelTime = getTravelTime(locationHashMap.get(nodeList.get(pos)),
                locationHashMap.get(nodeList.get(pos+1)));

        // If we reach earlier we have an option to wait for the client's window time=
        if(currentTime+travelTime < window.getStartTimeInSecond()){
            p = getPenaltyDP(pos + 1, nodeList,
                    currentTime + travelTime + getTerminalPenalty(currentTime, window) + 120);
        }
        // we must get penalty, may be it will be 0 if we reach in between client's window time==
        q = getPenaltyDP(pos+1, nodeList,
                currentTime+travelTime+120) + getTerminalPenalty(currentTime+travelTime, window);

        // setting up how we choose to go to the next terminal==========
        if(p <= q){
            timeStamp[pos+1] = 1;
            return p;
        }
        else{
            timeStamp[pos+1] = 2;
            return q;
        }
    }

    public void setTimeOfPositions(List<Integer> nodeList){
        Long currentTime = (long)startOfDuty;
        //adding headOffice=============
        nodeList.add(0, 0);

        //System.out.println("In setTimeOfPos: " + nodeList);

        for(int i=1; i<nodeList.size(); i++){
            Long travelTime = getTravelTime(locationHashMap.get(nodeList.get(i-1)),
                    locationHashMap.get(nodeList.get(i)));
            Long penaltyTime = getTerminalPenalty(currentTime+travelTime,
                    windowHashMap.get(nodeList.get(i)));

            // setting up current time according to our choice of visiting terminal========
            if(timeStamp[nodeList.get(i)] == 1) currentTime += (travelTime + penaltyTime);
            else currentTime += travelTime;

            timeOfPositions.put(nodeList.get(i), currentTime);
            currentTime += 120;
        }

        int lastNode = nodeList.get(nodeList.size()-1);
        int head = nodes+1;
        timeOfPositions.put(head, currentTime + getTravelTime(locationHashMap.get(lastNode), headOffice));

        // removing headOffice=======
        nodeList.remove(0);
    }

    public Long getTravelTime(Location A, Location B){
        Double speed = 50000.00/3600.00; // speed in m/s===

        Double distance = 1000.00 * Math.sqrt((A.latitude-B.longitude)*(A.latitude-B.longitude)
                + (A.latitude-B.latitude)*(A.latitude-B.latitude)); // distance in meter

        Long travelTime = Math.round(distance/speed); // time in second===

        return travelTime;
    }

    public int getTimeIn24(Long time){
        int hours = (int)(time/3600)*100;
        int minutes = (int)(time%3600)/60;
        return hours+minutes; // returned in 24h format
    }

    public Long getTerminalPenalty(Long currentTime, Window window){
        Long penaltyTime;

        // if we reach before window
        if(currentTime < window.getStartTimeInSecond()){
            penaltyTime = window.getStartTimeInSecond()-currentTime;
        }
        // if we reach after window
        else if(currentTime > window.getEndTimeInSecond()){
            penaltyTime = currentTime - window.getEndTimeInSecond();
        }
        // if we reach in between window
        else{
            penaltyTime = 0l;
        }

        return penaltyTime;
    }

}
