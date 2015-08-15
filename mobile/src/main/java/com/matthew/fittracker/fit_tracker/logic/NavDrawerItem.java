package com.matthew.fittracker.fit_tracker.logic;
/**
 * Created by dalton on 10/08/2015.
 * As each list item contains three elements icon, title, I would like create a model to represent each list row.
 */
public class NavDrawerItem {
    private String title;
    private int icon;

    public NavDrawerItem() {}

    public NavDrawerItem(String title, int icon){
        this.title = title;
        this.icon = icon;
    }
    public String getTitle(){
        return this.title;
    }

    public int getIcon(){
        return this.icon;
    }

    public void setTitle(String title){
        this.title = title;
    }
    public void setIcon(int icon){
        this.icon = icon;
    }
}