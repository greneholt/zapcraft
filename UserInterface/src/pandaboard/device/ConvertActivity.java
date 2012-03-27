package pandaboard.device;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.content.Intent;

public class ConvertActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    
    //public void selfDestruct(View view) {
    //	System.exit(0);
    //}
    
        Button next = (Button) findViewById(R.id.button1);
        next.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        		Intent myIntent = new Intent(view.getContext(), OptimizedRoute.class);
        		startActivityForResult(myIntent, 0);
        	}

        });
    }
    
    //android:onClick="listener"
    
    /*OnClickListener listener = new OnClickListener(){
        public void onClick(View v) {    
        	OptimizedRoute routewindow = new OptimizedRoute();
        	routewindow.setVisible(true);
        }
    };*/
}


	/*public class ButtonListener implements ActionListener 
	{
		//public void actionPerformed(Action e) 
		//{
		//	LiveData dialog=new LiveData();
		//	dialog.setVisible(true);
		//}

		public void onFailure(int arg0) {
			// TODO Auto-generated method stub
			System.exit(0);
		}

		public void onSuccess() {
			// TODO Auto-generated method stub
			
		}
	}
}*/
