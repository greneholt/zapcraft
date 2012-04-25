package sd.device;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LeftPanelFragment extends ListFragment{
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        RightPanelFragment viewer = new RightPanelFragment();
        viewer.button_number(position);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.right_fragment, viewer);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.list_items, R.layout.button_list));
    }
}
