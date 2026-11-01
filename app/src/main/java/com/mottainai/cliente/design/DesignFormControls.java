package com.mottainai.cliente.design;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mottainai.cliente.R;

public final class DesignFormControls {
    private DesignFormControls() {}
    public static void prepare(EditText input, boolean password) {
        Context context=input.getContext();
        ViewGroup parent=(ViewGroup)input.getParent();
        int index=parent.indexOfChild(input);
        ViewGroup.LayoutParams params=input.getLayoutParams();
        parent.removeViewAt(index);
        LinearLayout wrapper=new LinearLayout(context);wrapper.setOrientation(LinearLayout.VERTICAL);
        parent.addView(wrapper,index,params);
        FrameLayout box=new FrameLayout(context);
        wrapper.addView(box,new LinearLayout.LayoutParams(-1,-2));
        box.addView(input,new FrameLayout.LayoutParams(-1,-2));
        if(password) {
            input.setPaddingRelative(input.getPaddingStart(),input.getPaddingTop(),dp(context,60),input.getPaddingBottom());
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ImageButton eye=new ImageButton(context);
            eye.setImageResource(R.drawable.design_visibility);
            eye.setColorFilter(input.getCurrentTextColor());
            android.util.TypedValue ripple=new android.util.TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,ripple,true);
            eye.setBackgroundResource(ripple.resourceId);
            eye.setContentDescription(context.getString(R.string.design_show_password));
            FrameLayout.LayoutParams eyeParams=new FrameLayout.LayoutParams(dp(context,48),dp(context,48),Gravity.END|Gravity.CENTER_VERTICAL);
            eyeParams.setMarginEnd(dp(context,4));box.addView(eye,eyeParams);
            eye.setOnClickListener(v->{
                boolean visible=input.getTransformationMethod()!=null;
                int selection=input.getSelectionStart();
                input.setTransformationMethod(visible?null:PasswordTransformationMethod.getInstance());
                if(selection>=0)input.setSelection(Math.min(selection,input.length()));
                eye.setImageResource(visible?R.drawable.design_visibility_off:R.drawable.design_visibility);
                eye.setContentDescription(context.getString(visible?R.string.design_hide_password:R.string.design_show_password));
            });
        }
        TextView error=new TextView(context);error.setId(R.id.design_field_error);
        error.setTextColor(Color.rgb(233,99,99));error.setTextSize(13);
        error.setPadding(0,dp(context,6),0,0);error.setVisibility(View.GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        wrapper.addView(error,new LinearLayout.LayoutParams(-1,-2));
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){error.setVisibility(View.GONE);}
            public void afterTextChanged(Editable s){}
        });
    }
    public static void error(EditText field,String message) {
        ViewGroup wrapper=(ViewGroup)field.getParent().getParent();
        TextView error=wrapper.findViewById(R.id.design_field_error);
        if(error!=null){error.setText(message);error.setVisibility(View.VISIBLE);}
        field.requestFocus();
        field.post(()->field.requestRectangleOnScreen(new android.graphics.Rect(0,0,field.getWidth(),field.getHeight()+dp(field.getContext(),40)),false));
    }
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
}
