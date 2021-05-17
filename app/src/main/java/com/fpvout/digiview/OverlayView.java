package com.fpvout.digiview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class OverlayView extends ConstraintLayout {
    private final TextView textView;
    private final ImageView imageView;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(),R.layout.backdrop_view,this);

        textView = findViewById(R.id.backdrop_text);

        imageView = findViewById(R.id.backdrop_image);
    }

    public void hide(){
        setVisibility(View.GONE);
    }

    public void showOverlay(int textResourceId, OverlayStatus status){
        showOverlay(getContext().getString(textResourceId), status);
    }

    public void showOverlay(String text, OverlayStatus status){
        showInfo(text, status, 0.6f);
    }

    public void showOpaque(int textResourceId, OverlayStatus status){
        showOpaque(getContext().getString(textResourceId), status);
    }

    public void showOpaque(String text, OverlayStatus status){
        showInfo(text, status, 1f);
    }

    private void showInfo(String text, OverlayStatus status, float alpha){

        setVisibility(View.VISIBLE);
        setAlpha(alpha);

        textView.setText(text);

        int image = R.drawable.ic_goggles_white;
        switch(status){
            case Disconnected:
                image = R.drawable.ic_goggles_disconnected_white;
                break;
            case Error:
                image = R.drawable.ic_goggles_disconnected_red;
                break;
        }

        imageView.setImageResource(image);
    }
}
