package ch.abertschi.notiplay.view;

/**
 * Created by abertschi on 23.02.18.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * A FrameLayout that allow setting a delegate for intercept touch event
 * https://stackoverflow.com/questions/6841971/android-intercept-and-pass-on-all-touch-events
 */
public class InterceptTouchFrameLayout extends FrameLayout {
    private boolean mDisallowIntercept;

    public interface OnInterceptTouchEventListener {
        /**
         * If disallowIntercept is true the touch event can't be stealed and the return value is ignored.
         *
         * @see android.view.ViewGroup#onInterceptTouchEvent(android.view.MotionEvent)
         */
        boolean onInterceptTouchEvent(InterceptTouchFrameLayout view, MotionEvent ev, boolean disallowIntercept);

        /**
         * @see android.view.View#onTouchEvent(android.view.MotionEvent)
         */
        boolean onTouchEvent(InterceptTouchFrameLayout view, MotionEvent event);
    }

    private static final class DummyInterceptTouchEventListener implements OnInterceptTouchEventListener {
        @Override
        public boolean onInterceptTouchEvent(InterceptTouchFrameLayout view, MotionEvent ev, boolean disallowIntercept) {
            return false;
        }

        @Override
        public boolean onTouchEvent(InterceptTouchFrameLayout view, MotionEvent event) {
            return false;
        }
    }

    private static final OnInterceptTouchEventListener DUMMY_LISTENER = new DummyInterceptTouchEventListener();

    private OnInterceptTouchEventListener mInterceptTouchEventListener = DUMMY_LISTENER;

    public InterceptTouchFrameLayout(Context context) {
        super(context);
    }

    public InterceptTouchFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptTouchFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public InterceptTouchFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyle) {
        super(context, attrs, defStyleAttr, defStyle);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
        mDisallowIntercept = disallowIntercept;
    }

    public void setOnInterceptTouchEventListener(OnInterceptTouchEventListener interceptTouchEventListener) {
        mInterceptTouchEventListener = interceptTouchEventListener != null ? interceptTouchEventListener : DUMMY_LISTENER;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean stealTouchEvent = mInterceptTouchEventListener.onInterceptTouchEvent(this, ev, mDisallowIntercept);
        return stealTouchEvent && !mDisallowIntercept || super.onInterceptTouchEvent(ev);
//        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = mInterceptTouchEventListener.onTouchEvent(this, event);
        return handled || super.onTouchEvent(event);
    }
}
