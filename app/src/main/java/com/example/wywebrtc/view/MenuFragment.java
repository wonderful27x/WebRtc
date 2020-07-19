package com.example.wywebrtc.view;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.wywebrtc.R;
import com.example.wywebrtc.utils.DisplayUtil;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;


/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description 按钮Fragment
 * @license  BSD-2-Clause License
 */
public class MenuFragment extends Fragment implements View.OnClickListener{

    private TextView switchMute;
    private TextView switchHandsfree;
    private TextView powerCamera;
    private ImageView hangUp;
    private ImageView switchCamera;
    private RelativeLayout rootView;

    private boolean enableMute = false;
    private boolean enableHandsfree = true;
    private boolean enableCamera = true;

    private WebRtcInterface viewInterface;
    private Activity activity;

    public MenuFragment(WebRtcInterface viewInterface){
        this.viewInterface = viewInterface;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_fragment_layout,container,false);
        switchMute = view.findViewById(R.id.switchMute);
        switchCamera = view.findViewById(R.id.switchCamera);
        switchHandsfree = view.findViewById(R.id.switchHandsfree);
        powerCamera = view.findViewById(R.id.powerCamera);
        hangUp = view.findViewById(R.id.hangUp);
        rootView = view.findViewById(R.id.rootView);

        switchMute.setOnClickListener(this);
        switchCamera.setOnClickListener(this);
        switchHandsfree.setOnClickListener(this);
        powerCamera.setOnClickListener(this);
        hangUp.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.activity = getActivity();
        if (activity instanceof SingleChatActivity || activity instanceof SingleAudioActivity){
            rootView.setBackground(null);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.switchMute:
                enableMute = !enableMute;
                toggleMic(enableMute);
                viewInterface.switchMute(enableMute);
                break;
            case R.id.switchCamera:
                viewInterface.switchCamera();
                break;
            case R.id.switchHandsfree:
                enableHandsfree = !enableHandsfree;
                toggleSpeaker(enableHandsfree);
                viewInterface.switchHandsfree(enableHandsfree);
                break;
            case R.id.powerCamera:
                enableCamera = !enableCamera;
                toggleOpenCamera(enableCamera);
                viewInterface.powerCamera(enableCamera);
                break;
            case R.id.hangUp:
                viewInterface.hangUp();
                break;
                default:break;
        }
    }

    private void toggleMic(boolean isMicEnable) {
        if (isMicEnable) {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute_default);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            switchMute.setCompoundDrawables(null, drawable, null, null);
        } else {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_mute);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            switchMute.setCompoundDrawables(null, drawable, null, null);
        }
    }

    public void toggleSpeaker(boolean enableSpeaker) {
        if (enableSpeaker) {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            switchHandsfree.setCompoundDrawables(null, drawable, null, null);
        } else {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_hands_free_default);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            switchHandsfree.setCompoundDrawables(null, drawable, null, null);
        }
    }

    private void toggleOpenCamera(boolean enableCamera) {
        if (enableCamera) {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_open_camera_normal);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            powerCamera.setCompoundDrawables(null, drawable, null, null);
            powerCamera.setText(R.string.webrtc_close_camera);
            switchCamera.setVisibility(View.VISIBLE);
        } else {
            Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.webrtc_open_camera_press);
            if (drawable != null) {
                drawable.setBounds(0, 0, DisplayUtil.dip2px(activity, 60), DisplayUtil.dip2px(activity, 60));
            }
            powerCamera.setCompoundDrawables(null, drawable, null, null);
            powerCamera.setText(R.string.webrtc_open_camera);
            switchCamera.setVisibility(View.GONE);
        }
    }
}
