package com.handstalk.signdetect.listener;

import com.handstalk.signdetect.model.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);
}
