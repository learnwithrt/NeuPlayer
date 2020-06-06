package com.neusoft.neuplayer;

class Song {
    //Name is what I want to show on the list
    private String mName;
    //This is required for playing the media
    private String mPath;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public Song(String name, String path) {
        mName = name;
        mPath = path;
    }

    public Song() {
    }
}
