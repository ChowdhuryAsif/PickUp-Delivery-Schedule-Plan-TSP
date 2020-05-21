import sun.rmi.server.LoaderHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLOutput;
import java.util.*;

public class LocalSearch {
    int nodes;
    Long endOfDuty = 18*60*60l; // 6pm in second======
    Long startOfDuty = 6*60*60l; // 6am in second======
    HashMap<Integer, Location> locationHashMap;
    HashMap<Integer, Window> windowHashMap;
    HashMap<Integer, Long> timeOfPositions;
    List<Integer> nodeList;
    HashSet<List<Integer>> stateHasTaken;
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

        double currentTemperature = initialTemperature;

        List<Integer> currentPath = nodeList;
        final List<Integer> bestPath = new ArrayList();
        final HashMap<Integer, Long> bestTimeOfPosition = new HashMap();
        currentPath.forEach(node -> bestPath.add(node));

        Long currentPenalty = getPenaltyDP(0, currentPath,
                startOfDuty+getTravelTime(headOffice, locationHashMap.get(nodeList.get(0))));

        Long previousPenalty;
        Long bestPenalty = currentPenalty;

        int iteration = 1;

        while(currentTemperature > 0 && bestPenalty > 0){
            previousPenalty = currentPenalty;
            List<Integer> nextRandomPath = new ArrayList();
            currentPath.forEach(node -> nextRandomPath.add(node));
            Collections.shuffle(nextRandomPath);

            currentPenalty = getPenaltyDP(0, nextRandomPath,
                    startOfDuty+getTravelTime(headOffice, locationHashMap.get(nodeList.get(0))));

            double random = Math.random();
            if(random < acceptanceProbability(currentPenalty, previousPenalty, currentTemperature)) {
                //changes are acceptable===
                if (currentPenalty < bestPenalty) {
                    bestPenalty = currentPenalty;
                    bestPath.clear();
                    nextRandomPath.forEach(node -> bestPath.add(node));
                    bestTimeOfPosition.clear();
                    timeOfPositions.forEach((node, time) -> bestTimeOfPosition.put(node, time));
                    timeOfPositions.clear();
                    //System.out.println("Iteration: " + iteration + ": " + bestPath + " Penalty: " + bestPenalty);
                }
            }
            currentTemperature -= coolingRate;
            iteration++;
        }

        System.out.println(bestPath);
        System.out.println(headOffice.toString() + " Time: " + getTimeIn24(startOfDuty));
        bestPath.forEach(node -> System.out.println(locationHashMap.get(node).toString() + " Time: " + getTimeIn24(bestTimeOfPosition.get(node))));
        int lastNode = bestPath.get(bestPath.size()-1);
        Long travelTime = getTravelTime(locationHashMap.get(lastNode), headOffice);
        System.out.println(headOffice.toString() + " Time: " +
                getTimeIn24(bestTimeOfPosition.get(lastNode)+travelTime));

        System.out.println("Total Penalty: " + bestPenalty);
    }

    // wrong approach=== not choosing a pickup man should wait or get penalty
//    public Long penalty(List<Integer> nodeList){
//        Long penaltyTime;
//        Long travelTime;
//        Long currentTime = 6*60*60l;
//        Long totalPenalty = 0l;
//
//        Location prevLocation = headOffice;
//        int idx = 0;
//        for( ; idx<nodes; idx++){
//            int node = nodeList.get(idx);
//            Location nextLocation = locationHashMap.get(node);
//            travelTime = getTravelTime(prevLocation, nextLocation);
//
//            if(currentTime+travelTime+2 > endOfDuty) break; // duty time finished
//
//            currentTime += travelTime;
//            Window window = windowHashMap.get(node);
//            penaltyTime = getTerminalPenalty(currentTime, window);
//
//            timeOfPositions.put(node, currentTime);
//            currentTime += 120; // adding picking time 2*60 = 120s
//            totalPenalty += penaltyTime;
//            //System.out.println(prevLocation  + " to " + nextLocation + " Current Time: " + getTimeIn24(currentTime));
//            prevLocation = nextLocation;
//        }
//        int lastNode = nodeList.get(nodeList.size()-1);
//        Location lastNodeLocation = locationHashMap.get(lastNode);
//        currentTime += getTravelTime(headOffice, lastNodeLocation);
//        timeOfPositions.put(nodes+1, currentTime);
//        totalPenalty += (nodes-idx-1) * 10000; // penalty for not able to pickup packages on duty time
//        //System.out.println(lastNodeLocation  + " to " + headOffice + " Current Time: " + getTimeIn24(currentTime));
//        return totalPenalty;
//    }

    public Long getPenaltyDP(int pos, List<Integer> nodeList, Long currentTime){
        // I am using dp because I may wait for a node.window.startTime() or I may get penalty
        // in which way is best I will take it
        // so I am choosing the best by waiting and getting penalty

        if(pos == nodeList.size()-1){
            timeOfPositions.put(nodeList.get(pos), currentTime);
            return 0l;
        }
        if(pos == 0){
            if(currentTime+getTravelTime(headOffice, locationHashMap.get(nodeList.get(pos)))+120 > endOfDuty){
                return nodeList.size() * 10000l;
            }
        }
        else{
            if(currentTime+getTravelTime(locationHashMap.get(nodeList.get(pos-1)),
                    locationHashMap.get(nodeList.get(pos)))+120 > endOfDuty){
                return (nodeList.size()-pos+1) * 10000l;
            }
        }

        Window window = windowHashMap.get(nodeList.get(pos));
        Long p=Long.MAX_VALUE, q=Long.MAX_VALUE;
        Long travelTime;
        if(pos == 0){
            travelTime = getTravelTime(headOffice, locationHashMap.get(nodeList.get(pos)));
        }
        else{
            travelTime = getTravelTime(locationHashMap.get(nodeList.get(pos)),
                    locationHashMap.get(nodeList.get(pos+1)));
        }

        if(currentTime+travelTime < window.getStartTimeInSecond()){
            p = getPenaltyDP(pos + 1, nodeList,
                    currentTime + travelTime + getTerminalPenalty(currentTime, window) + 120);
        }
        q = getPenaltyDP(pos+1, nodeList,
                currentTime+travelTime+120) + getTerminalPenalty(currentTime+travelTime, window);

        timeOfPositions.put(nodeList.get(pos), currentTime);
        return Math.min(p, q);
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
        if(currentTime < window.getStartTimeInSecond()){
            penaltyTime = window.getStartTimeInSecond()-currentTime;
        }
        else if(currentTime > window.getEndTimeInSecond()){
            penaltyTime = currentTime - window.getEndTimeInSecond();
        }
        else{
            penaltyTime = 0l;
        }

        return penaltyTime;
    }

}
