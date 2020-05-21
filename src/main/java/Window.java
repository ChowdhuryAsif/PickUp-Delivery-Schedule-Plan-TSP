import java.util.Objects;

public class Window {

    // times are converted in second
    int startTime;
    int endTime;

    public Window(int startTime, int endTime) {
        this.startTime = ((startTime/100)*3600) + ((startTime%100)*60);
        this.endTime = ((endTime/100)*3600) + ((endTime%100)*60);
    }

    public int getStartTime() {
        int hours = (startTime/3600)*100;
        int minutes = (startTime%3600)/60;
        return hours+minutes; // returned in 24h format
    }

    public int getEndTime() {
        int hours = (endTime/3600)*100;
        int minutes = (endTime%3600)/60;
        return hours+minutes; // returned in 24h format
    }

    public void setStartTime(int startTime) {
        this.startTime = ((startTime/100)*3600) + ((startTime%100)*60);
    }

    public void setEndTime(int endTime) {
        this.endTime = ((endTime / 100) * 3600) + ((endTime % 100) * 60);
    }

    public int getEndTimeInSecond(){
        return endTime;
    }

    public int getStartTimeInSecond(){
        return startTime;
    }

    @Override
    public String toString() {
        return String.format("StartTime: %d, EndTime: %d", getStartTime(), getEndTime());
    }
}
