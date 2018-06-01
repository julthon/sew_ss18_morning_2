package at.tugraz.recipro.views;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.AttributeSet;
import android.widget.AdapterView;

import com.plumillonforge.android.chipview.Chip;
import com.plumillonforge.android.chipview.ChipView;
import com.plumillonforge.android.chipview.OnChipClickListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OurChipView extends ChipView {

    List<OnSomethingChangedListener> eventListeners = new LinkedList<>();

    @Override
    public void add(Chip chip) {
        if(!mAdapter.getChipList().contains(chip)) {
            super.add(chip);
            for (OnSomethingChangedListener listener : eventListeners)
                listener.onSomethingChanged();
        }
    }

    public void addOnSomethingChangedListener(OnSomethingChangedListener listener) {
        eventListeners.add(listener);
    }

    private void addChipClickListener() {
        super.setOnChipClickListener(new OnChipClickListener() {
            @Override
            public void onChipClick(Chip chip) {
                OurChipView.super.remove(chip);
                for (OnSomethingChangedListener listener : eventListeners)
                    listener.onSomethingChanged();
            }
        });
    }

    public OurChipView(Context context) {
        super(context);
        addChipClickListener();
    }

    public OurChipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addChipClickListener();
    }

    public OurChipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addChipClickListener();
    }

    public <T> List<OurTagImplementation<T>> getListOfType(OurTagImplementation.TagType tagType) {
        ArrayList<OurTagImplementation<T>> tags = new ArrayList<>();
        for (Chip chip : super.getChipList()) {
            OurTagImplementation<T> c = (OurTagImplementation<T>) chip;
            if (c.getTagType() == tagType || tagType == null)
                tags.add(c);
        }
        return tags;
    }

    public interface OnSomethingChangedListener{
        void onSomethingChanged();
    }
}
