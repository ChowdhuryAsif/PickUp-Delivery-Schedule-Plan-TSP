import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class GeneticAlgorithm {
    int nodes;
    int startOfDuty = 6*60*60; // 6am in second======
    int endOfDuty = 18*60*60; // 6pm in second======
    List<Integer> nodeList;
    List<List<Integer>> population;
    HashMap<Integer, Location> locationHashMap;
    HashMap<Integer, Window> windowHashMap;
    Double fitness[];

    List<Integer> bestPathEver;
    Long bestFitnessEver;

    Location headOffice = new Location(23.7938636,90.4053257); // AR Tower location

    public GeneticAlgorithm() {
        locationHashMap = new HashMap();
        windowHashMap = new HashMap();
        nodeList = new ArrayList();
        population = new ArrayList();
        bestPathEver = new ArrayList();
        bestFitnessEver = Long.MAX_VALUE;

        takeInput();
    }

    public void takeInput(){
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

            // adding locations of front and back headOffice as 0, nodes+1
            locationHashMap.put(0, headOffice);
            locationHashMap.put(nodes+1, headOffice);

            // adding window of front and back headOffice as 0, nodes+1
            windowHashMap.put(0, new Window(startOfDuty, endOfDuty));
            windowHashMap.put(nodes+1, new Window(startOfDuty, endOfDuty));

            System.out.println("NodeList: " + nodeList.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generatePopulation(int n){
        fitness = new Double[n]; // we will record fitness of each population===

        for(int i=1; i<=n; i++){
            List<Integer> randomPath = new ArrayList();
            nodeList.forEach(node -> randomPath.add(node));
            Collections.shuffle(randomPath);
            population.add(randomPath);
        }
    }

    public void getFitnessOfPopulation(List<List<Integer>> population){
        for(int i=0; i<population.size(); i++){
            List<Integer> path = population.get(i);

            Long fitness = calculateFitness(path);
            if(fitness < bestFitnessEver){
                bestFitnessEver = fitness;
                bestPathEver = path;
            }
            // I have inverted the fitness so that the lower value become higher and higher one become lower
            // this is for calculating the probability of acceptance=======
            this.fitness[i] = 1.00/fitness; //obviously here fitness can't be 0
            //System.out.println(i + " " + this.fitness[i]);
        }
    }

    public  void printPopulation(List<List<Integer>> population){
        System.out.println("Population:");
        population.forEach(path -> System.out.println(path.toString()));
    }

    public void startSearch(){

        generatePopulation(10);
        //geneticAlgorithm.printPopulation(geneticAlgorithm.population);
        int k = 100;
        while(k > 0){
            getFitnessOfPopulation(population);

            //System.out.println("First Generation:");
            //population.forEach(path -> System.out.println(path));




            normalizeFitness();
            nextGeneration();

            System.out.println(101-k + "th Generation:");
            //population.forEach(path -> System.out.println(path));

            System.out.println("BestPathEver: " + bestPathEver);
            System.out.println("Best Fitness: " + bestFitnessEver);
            k--;
        }

    }

    public Long calculateFitness(List<Integer> path){
        // adding headOffice at the front and the back to the path
        // front headOffice is numbered as 0 and back headOffice is numbered as nodes+1;
        path.add(0, 0);
        path.add(nodes+1);

        Long currentTime = (long) startOfDuty;
        Long fitness = 0l;
        int packagingTime = 120; // 2 minutes in seconds

        int errorOccurredAtPos = -1;

        for(int i=1; i<path.size()-1; i++){
            Location terminalA = locationHashMap.get(path.get(i-1));
            Location terminalB = locationHashMap.get(path.get(i));

            Window windowB = windowHashMap.get(path.get(i));

            Long travelTime = getTravelTime(terminalA, terminalB);
            Long penaltyTime = getTerminalPenalty(currentTime+travelTime, windowB);

            if(currentTime+travelTime < windowB.getStartTimeInSecond()){
                // if can't reach headOffice by picking next package before endOfDuty===
                if(currentTime+travelTime+penaltyTime+packagingTime+getTravelTime(terminalB, headOffice) > endOfDuty){
                    errorOccurredAtPos = i-1; // I can't reach here (i.e ith node) ===
                    break;
                }
            }
            if(errorOccurredAtPos != -1){
                // removing front and back headOffice from the path;
                path.remove(0);
                path.remove(path.size()-1);

                // penalty occurred for the rest of the clients that can't pick in duty time
                fitness += (path.size()-errorOccurredAtPos-1) * 10000;
                return fitness;
            }

            // if I go to the last node of path which is headOffice
            // I have to add another condition here to ignore penaltyTime and packaging time
            // so I will calculate this at the end of this method
            fitness += travelTime + penaltyTime + packagingTime;
            currentTime += travelTime + penaltyTime + packagingTime;
        }
        // removing front and back headOffice from the path;
        path.remove(0);
        path.remove(path.size()-1);

        int lastNode = path.get(path.size()-1);
        Location lastNodeLocation = locationHashMap.get(lastNode);

        // adding travelTime from lastNode to headOffice ========
        fitness += getTravelTime(lastNodeLocation, headOffice);

        return fitness;
    }

    public void normalizeFitness(){
        Double sum = 0d;

        for(int i=0; i<fitness.length; i++){
            sum += fitness[i];
        }

        for(int i=0; i<fitness.length; i++){
            fitness[i] /= sum;
        }
    }

    public void nextGeneration(){
        List<List<Integer>> newPopulation = new ArrayList();

        for(int i=0; i<population.size(); i++){
            List<Integer> path = pickOne();
            mutate(path);
            newPopulation.add(path);
        }
        population.clear();
        population = newPopulation;
    }

    public List<Integer> pickOne(){
        int idx = 0;
        Double random = new Random().nextDouble();

        while(random > 0.000){
            random -= fitness[idx++];
        }
        idx--;
        return population.get(idx);
    }

    public void mutate(List<Integer> path){
        int idxA = new Random().nextInt(path.size());
        int idxB = new Random().nextInt(path.size());

        swap(path, idxA, idxB);
    }

    public void swap(List<Integer> lst, int idxA, int idxB){
        int temp = lst.get(idxA);
        lst.set(idxA, lst.get(idxB));
        lst.set(idxB, temp);
    }

    public Long getTravelTime(Location A, Location B){
        Double speed = 50000.00/3600.00; // speed in m/s===

        Double distance = 1000.00 * Math.sqrt((A.latitude-B.longitude)*(A.latitude-B.longitude)
                + (A.latitude-B.latitude)*(A.latitude-B.latitude)); // distance in meter

        Long travelTime = Math.round(distance/speed); // time in second===

        return travelTime;
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