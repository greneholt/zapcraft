package sd.device;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RightPanelFragment extends Fragment {
    private View viewer = null;
    private int theid = R.layout.blank;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	viewer = inflater.inflate(theid, container, false);
        return viewer;
    }
    
    public void button_number(int pos){
    	switch (pos){
    		case 0:	theid = R.layout.live_data;
    				break;
    		case 1: theid = R.layout.blank;
    				break;
    		case 2: theid = R.layout.live_data;
    				break;
    		case 3: theid = R.layout.blank;
    				break;
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }
}