package de.freesoccerhdx.simplesocket;

public class Pair<T, T1> {

    private T first;
    private T1 second;

    public static <T, T1> Pair<T, T1> of(T first, T1 second){
        return new Pair<>(first,second);
    }

    public Pair(T first, T1 second){
        this.first = first;
        this.second = second;
    }

    public T getFirst(){
        return first;
    }

    public T1 getSecond(){
        return second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
