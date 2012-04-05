// Copyright (c) 2010, Freddy Martens (http://atstechlab.wordpress.com), 
// MindTheRobot (http://mindtherobot.com/blog/)  and contributors
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without modification, 
// are permitted provided that the following conditions are met:
//
//	* Redistributions of source code must retain the above copyright notice, 
//	  this list of conditions and the following disclaimer.
//	* Redistributions in binary form must reproduce the above copyright notice, 
//	  this list of conditions and the following disclaimer in the documentation 
//	  and/or other materials provided with the distribution.
//	* Neither the name of Ondrej Zara nor the names of its contributors may be used 
//	  to endorse or promote products derived from this software without specific 
//	  prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY 
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
// EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.freddymartens.android.widgets;

import com.freddymartens.android.widgets.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;

public class TestActivity extends Activity implements OnClickListener, OnLongClickListener {

	Button btnHotter;
	Button btnColder;
	Gauge meter1;
	Gauge meter2;
	float temperatureC = (float)17.7;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        btnHotter = (Button) findViewById(R.id.btnHotter);
        btnColder = (Button) findViewById(R.id.btnColder);
        meter1    = (Gauge) findViewById(R.id.meter1);
        meter2    = (Gauge) findViewById(R.id.meter2);
        
        btnHotter.setOnClickListener(this);
        btnColder.setOnClickListener(this);
        
    }

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.btnColder:
			meter1.setValue(--temperatureC);
			meter2.setValue(--temperatureC);
			break;
		case R.id.btnHotter:
			meter1.setValue(++temperatureC);
			meter2.setValue(++temperatureC);
			break;
		}
		return;
	}

	@Override
	public boolean onLongClick(View view) {
		switch(view.getId()) {
		case R.id.btnColder:
			temperatureC = temperatureC - 10;
			meter1.setValue(temperatureC);
			meter2.setValue(temperatureC);
			break;
		case R.id.btnHotter:
			temperatureC = temperatureC + 10;
			meter1.setValue(temperatureC);
			meter2.setValue(temperatureC);
			break;
		}
		return true;
	}
}