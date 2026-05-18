package com.libambu.dataagent.entity.pjo;

import java.util.List;


public class ActorMovies {
    private String name;
    //private List<String> likePeople;
    private List<String> Movies;
    //private List<String> a;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public List<String> getLikePeople() {
//        return likePeople;
//    }
//
//    public void setLikePeople(List<String> likePeople) {
//        this.likePeople = likePeople;
//    }
    public List<String> getMovies() {
        return Movies;
    }
    public void setMovies(List<String> movies) {
        Movies = movies;
    }
//    public List<String> getA() {
//        return a;
//    }
//    public void setA(List<String> a) {
//        this.a = a;
//    }
}
