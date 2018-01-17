package hidayahco.kreasigambar;

import java.util.LinkedList;

import android.view.MotionEvent;

public class prosesgambarbersih {
    public static boolean DEBUG = true;
    
    public static boolean PRECISE_STYLUS_INPUT = true;

    public static interface Plotter {
        public void plot(rekamsentuhan s);
    }

    LinkedList<rekamsentuhan> mRekamsentuhans; // NOTE: newest now at front
    int mBufSize;
    Plotter mPlotter;
    rekamsentuhan tmpRekamsentuhan = new rekamsentuhan();
    private float mPosDecay;
    private float mPressureDecay;

    public prosesgambarbersih(int size, float posDecay, float pressureDecay, Plotter plotter) {
        mRekamsentuhans = new LinkedList<rekamsentuhan>();
        mBufSize = size;
        mPlotter = plotter;
        mPosDecay = (posDecay >= 0 && posDecay <= 1) ? posDecay : 1f;
        mPressureDecay = (pressureDecay >= 0 && pressureDecay <= 1) ? pressureDecay : 1f;
    }

    public rekamsentuhan filteredOutput(rekamsentuhan out) {
        if (out == null) out = new rekamsentuhan();
        
        float wi = 1, w = 0;
        float wi_press = 1, w_press = 0;
        float x = 0, y = 0, pressure = 0, size = 0;
        long time = 0;
        for (rekamsentuhan pi : mRekamsentuhans) {
            x += pi.x * wi;
            y += pi.y * wi;
            time += pi.time * wi;
            
            pressure += pi.pressure * wi_press;
            size += pi.size * wi_press;

            w += wi;
            wi *= mPosDecay; // exponential backoff

            w_press += wi_press;
            wi_press *= mPressureDecay;

            if (PRECISE_STYLUS_INPUT && pi.tool == MotionEvent.TOOL_TYPE_STYLUS) {
                // just take the top one, no need to average
                break;
            }
        }

        out.x = x / w;
        out.y = y / w;
        out.pressure = pressure / w_press;
        out.size = size / w_press;
        out.time = time;
        out.tool = mRekamsentuhans.get(0).tool;
        return out;
    }

    public void add(MotionEvent.PointerCoords c, long time) {
    	addNoCopy(new rekamsentuhan(c, time));
    }
    
    public void add(rekamsentuhan c) {
    	addNoCopy(new rekamsentuhan(c));
    }
    
    protected void addNoCopy(rekamsentuhan c) {
        if (mRekamsentuhans.size() == mBufSize) {
            mRekamsentuhans.removeLast();
        }

        mRekamsentuhans.add(0, c);

        tmpRekamsentuhan = filteredOutput(tmpRekamsentuhan);
        mPlotter.plot(tmpRekamsentuhan);
    }

    public void add(MotionEvent.PointerCoords[] cv, long time) {
        for (MotionEvent.PointerCoords c : cv) {
            add(c, time);
        }
    }

    public void finish() {
        while (mRekamsentuhans.size() > 0) {
            tmpRekamsentuhan = filteredOutput(tmpRekamsentuhan);
            mRekamsentuhans.removeLast();
            mPlotter.plot(tmpRekamsentuhan);
        }

        mRekamsentuhans.clear();
    }
}


