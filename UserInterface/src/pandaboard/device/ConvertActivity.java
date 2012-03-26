package pandaboard.device;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class ConvertActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void selfDestruct(View view) {
        System.exit(0);
    }
}