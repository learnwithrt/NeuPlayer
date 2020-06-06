package com.neusoft.neuplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ListView mSongListView;
    private LinearLayout mControls;
    private TextView mDoneTime;
    private TextView mTotalTime;
    private ImageButton mPlayPauseButton;
    private SeekBar mSeekBar;
    //members to provide what permissions are required by this app
    private static final String[] PERMISSIONS_REQUIRED={
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    //how many permissions are required
    private static final int PERMISSIONS_COUNT=PERMISSIONS_REQUIRED.length;
    //LIST for keeping the file names/ paths
    ArrayList<String> mSongList;//this will just contain the name for ListView
    ArrayList<Song> mSongs;//This will contain the name and path
    private MediaPlayer player=null;
    private boolean audioPlaying=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSongListView=findViewById(R.id.audio_list);
        mControls=findViewById(R.id.controls);
        mDoneTime=findViewById(R.id.done_time);
        mTotalTime=findViewById(R.id.total_time);
        mSeekBar=findViewById(R.id.audio_seek);
        mPlayPauseButton=findViewById(R.id.play_pause_button);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)//M is for marshmallow
    private boolean checkPermissionsDenied(){
        for(int i=0;i<PERMISSIONS_COUNT;i++){
            if(checkSelfPermission(PERMISSIONS_REQUIRED[i])!= PackageManager.PERMISSION_GRANTED){
                return true;// yes the permissions are denied
            }
        }
        return false;//this means the permissions are granted(Not denied)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //what happened when the user was requested for these permissions
        if(checkPermissionsDenied()){//the user denied the permissions
            //Exit the application
            ((ActivityManager)this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
        }
        else{//what to do if the user accepts the permissions
            onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Request permissions only if the version is more than23(M) && permissions are denied
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M && checkPermissionsDenied()){
            requestPermissions(PERMISSIONS_REQUIRED,1234);
            return;
        }
        // Work for the media player
        //Do this if the media player is not created
        if(player==null){
            mSongList=new ArrayList<>();
            mSongs=new ArrayList<>();
            //get the list
            populateSongs();
            //associate this list with list view
            ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(getBaseContext()
            ,android.R.layout.simple_list_item_1,mSongList);
            mSongListView.setAdapter(arrayAdapter);
            mSongListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //get the path of the item clicked
                    //find the object which was clicked
                    String name=mSongList.get(position);
                    for( int i=0;i<mSongListView.getChildCount();i++){
                        if(i==position)
                            mSongListView.getChildAt(i).setBackgroundColor(Color.BLUE);
                        else
                            mSongListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                    }
                    //need the path
                    String path=mSongs.get(position).getPath();
                    playAudio(path);
                }
            });
            mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(audioPlaying){//song is being played now
                        player.pause();
                        //change the image on the button to play
                        mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                    else{//in pause state
                        player.start();
                        mPlayPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    }
                    //change the state of audioPlaying
                    audioPlaying=!audioPlaying;
                }
            });
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //User has finished the seek
                    //seek the player to this position
                    player.seekTo(seekBar.getProgress());
                    //change the time on text view
                    int time=player.getCurrentPosition();
                    long minutes=TimeUnit.MILLISECONDS.toMinutes(time);
                    mDoneTime.setText(String.format(Locale.getDefault(),"%d min, %d sec",
                            minutes,
                            TimeUnit.MILLISECONDS.toSeconds(time)-
                                    TimeUnit.MINUTES.toSeconds(minutes)));//format this as minutes and seconds
                }
            });
        }
    }

    private void playAudio(String path) {
        //Check again if the media player is created already
        if(player==null){
            player=new MediaPlayer();
        }
        else{
            //I want to reset the player
            player.stop();
            player.reset();
        }
        try {
            player.setDataSource(path);
            //tell which file to play. Prepare the player and start
            player.prepare();
            player.start();// start the media
            mControls.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.VISIBLE);
            int endTime=player.getDuration();
            long minutes=TimeUnit.MILLISECONDS.toMinutes(endTime);
            mTotalTime.setText(String.format(Locale.getDefault(),"%d min, %d sec",
                    minutes,
                    TimeUnit.MILLISECONDS.toSeconds(endTime)-
                    TimeUnit.MINUTES.toSeconds(minutes)));//format this as minutes and seconds
            mSeekBar.setMax(player.getDuration());
            //update the seek bar and the time when the song is playing
            //create a new thread
            new Thread(){
                public void run(){
                    int sPos=0;
                    while(sPos<player.getDuration()){//until the song is playing
                        //update every 500 milliseconds
                        try {
                            Thread.sleep(500);//make this thread wait 500 ms
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sPos+=500;
                        runOnUiThread(new Runnable() {//do this on the original thread
                            @Override
                            public void run() {
                                mSeekBar.setProgress(player.getCurrentPosition());
                                int currentTime=player.getCurrentPosition();
                                long minutes=TimeUnit.MILLISECONDS.toMinutes(currentTime);
                                mDoneTime.setText(String.format(Locale.getDefault(),"%d min, %d sec",
                                        minutes,
                                        TimeUnit.MILLISECONDS.toSeconds(currentTime)-
                                                TimeUnit.MINUTES.toSeconds(minutes)));//format this as minutes and seconds
                            }
                        });
                    }
                }
            }.start();//start this thread

        } catch (IOException e) {//what if the file on this path cannot be found?
            e.printStackTrace();
        }
    }

    private void populateSongs() {
        mSongList.clear();
        //get the songs from these directories( Downloaded music or the files in Music directory)
        getAudios(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        getAudios(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
    }

    //audios/ files from a particular path (folder, file)
    private void getAudios(File dir){
        File root=new File(dir.getAbsolutePath());// read the difference between absolute and relative path
        File[] list=root.listFiles();//get the list of files and directories in this directory
        //check if the list is empty
        if(list==null)
            return;// a condition to stop recursion
        for(File f:list){
            if(f.isDirectory()){
                getAudios(f);//recursively call the getAudios() to get files in this directory
            }
            else{
                //only audios(mp3, wav)
                if(f.getName().endsWith(".mp3")||f.getName().endsWith(".wav")){
                    //add the name to the list
                    mSongList.add(f.getName().replace(".mp3","")
                            .replace(".wav",""));// Delete .mp3 or .wav from the name
                    mSongs.add(new Song(f.getName(),f.getPath()));
                }
            }
        }
    }
}
