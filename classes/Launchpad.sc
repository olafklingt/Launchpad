
Launchpad{
	classvar <>midiout;
	classvar <>pads;//2DARRAY
	classvar nonr,nofr,cor;//MIDI RESPONDER FUNCTIONS
	*initClass{
		Launchpad.pads=Array2D.fromArray(9,9,
			Array.fill(81,{|i|LPPad .new((i/9).asInteger,i%9)}));
		// Launchpad.initMIDI;
	}
	*initMIDI{
		var non,nof,co,midisource,mididestination;
		non={|src,chan,num,val|
			var x=num%16;
			var y=(num-x)/16;
			if(val===127,{
				pads.at(x,y+1).responseOn;
				},{
				pads.at(x,y+1).responseOff(pads.at(x,y+1));
			});
		};
		nof={|src,chan,num,val|
			var x=num%16;
			var y=(num-x)/16;
			pads.at(x,y+1).responseOff(pads.at(x,y+1));
		};
		co={|src,chan,num,val|
			var x=num-104;
			if((val%2)===1,{
				pads.at(x,0).responseOn;
				},{
				pads.at(x,0).responseOff(pads.at(x,0));
			})
		};
		MIDIClient.init;
		// MIDIClient.initialized
		midisource=MIDIClient.sources.detect{|a|a.name.contains("Launchpad")};
		if(midisource.notNil,{
			mididestination=MIDIClient.destinations.detect{|a|a.name.contains("Launchpad")};
			MIDIIn.connect(0, midisource.uid);
			midiout=MIDIOut(0, mididestination.uid);
			MIDIOut.connect(0, mididestination.uid);
			nonr=NoteOnResponder(non,midisource.uid,swallowEvent:true);
			nofr=NoteOffResponder(non,midisource.uid,swallowEvent:true);
			cor=CCResponder(co,midisource.uid,swallowEvent:true);
			Launchpad.setupLP;
			^ 1;
			},{
				"Launchpad Not available".postln;
				^nil;
		})
	}
	*resetPads{
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		pads.do{|i|i.reset};
		midiout.control(0,0,0);
	}
	*resetLED{
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		//reset
		pads.do{|i|i.setLED(LPLED.black)};
		midiout.control(0,0,0);
	}
	*xyMode{
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.control(0,0,1);
	}
	*drumMode{
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.control(0,0,2);
	}
	*setContrast{|num=1,denum=6|
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.control(0,30,((16*(num-1))+(denum-3)));
	}
	*setupLP{
		this.resetPads;
		this.xyMode;
		this.setContrast;
	}
	*xyMIDINode{|x,y|
		^(x+(y*16))
	}
	*pos2CC{|pos|
		^(104+pos)
	}
	*getRange{|fx,fy,tx,ty|
		var ttx,tty;
		//ttx=if(fx>=(tx?0),{fx+1},{tx+1});
		//tty=if(fy>=(ty?0),{fy+1},{ty+1s});
		ttx=tx?fx;
		tty=ty?fy;
		^LPRange(pads.copyRange(fx,fy,ttx,tty));
	}
	//simply set LEDs
	*setLED{|x,y,val|
		if(y==0,{
			Launchpad.setTopLED(x,y,val);
		},{
			Launchpad.setMatrixLED(x,y,val);
		})
	}
	*setMatrixLED{|x,y,val|
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.noteOn(0, Launchpad.xyMIDINode(x,y-1), val);
		}
	*setTopLED{|x,y,val|
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.control(0, Launchpad.pos2CC(x), val);
		}
	*setPlayLED{|x,y,val|
		midiout.isNil.if{"Launchpad Not available".postln;^this};
		midiout.noteOn(0, Launchpad.xyMIDINode(8,y), val);
	}
	//set LEDs for initialisation
	*setAllLED{|ledarray|
		"todo".postln;
	}
	//doublebuffering?
	//flashing?
}

LPRange{
	var <>pads;
	*new{|b|
		^super.new.init(b);
	}
	init{|b|
		pads=b;
	}
	getRange{|fx,fy,tx,ty| // this is only to get sub-ranges
		var ttx,tty,out;
		ttx=tx?fx;
		tty=ty?fy;
		"ERROR: IS THIS BEFORE THE ERROR".debug(\before_copy);
		out=LPRange(pads.copyRange(fx,fy,ttx,tty).debug(\copiedrange));
		"ERROR: IS THIS AFTER THE ERROR".debug(\after_copy);
		^out;
	}
	setLED{|l|
		pads.do{|i|i.setLED(l)};
	}
}

LPPad{
	var <>x,<>y;
	var curvel;
	var stack;
	var responseOffObject;
	setLED{|l|
		var newvel=l.vel;
		if(curvel!=newvel){
			Launchpad.setLED(x,y,newvel);
			curvel=newvel;
		}
	}
	updateLED{|view,l|
		var index=stack.detectIndex{|a|a.view==view};
		if((index===stack.lastIndex)&&index.notNil){
			l.isNil.if{
				if(index>0){
					l=stack.collect(_.led).removeEvery([nil]).last?LPLED.black;
				}{
					l=LPLED.black;
				}
			}{
				stack.last.led=l;
			};
			this.setLED(l);
		}{
			if(index.notNil){ // i added this because i am not sure if a change in the back ground would get lost otherwise
				stack[index].led=l;
			}
		}
	}
	*new{|x,y|
		^super.new.init(x,y);
	}
	responseOn{
		if(stack.size>0){
			var dep=stack.last.view;
			dep.responseOn(this,stack.last.buttonid);
			responseOffObject=[dep,stack.last.buttonid];
		}
	}
	responseOff{
		responseOffObject!?{responseOffObject[0].responseOff(this,responseOffObject[1])};
		// if(stack.size>0){
		// 	var dep=stack.last.view;
		// 	dep.responseOff(this,stack.last.buttonid);
		// }
	}

	init{|ax,ay|
		x=ax;
		y=ay;
		this.reset;
	}
	reset{
		curvel=LPLED.black.vel;
		stack=List.new;
	}
	add{|that,led,buttonid|
		var index=stack.detectIndex{|a|a.view==that};
		index.notNil.if{stack.removeAt(index)};
		led=led??{try{stack.last.led}??LPLED.black};
		stack.add((view:that,led:led,buttonid:buttonid));
		this.setLED(led);
	}
	remove{|that,updateLEDs=true|
		var index=stack.detectIndex{|a|a.view==that};
		var lastIndex=stack.lastIndex;
		index.notNil.if{stack.removeAt(index)};
		if(index===lastIndex){
			if(updateLEDs){
				var l=stack.collect(_.led).removeEvery([nil]).last;
				l.notNil.if{this.setLED(l)}{this.setLED(LPLED.black)};
			}
		}
	}
}


LPLED{
	var <>green,<>red,clear,copy;
	*new{|green=0,red=0,clear=1,copy=1|
		^super.new.init(green.min(3),red.min(3),clear,copy);
	}
	*black{^this.new(0)}
	*green{|s=3|s=s.min(3);^this.new(s)}
	*red{|s=3|s=s.min(3);^this.new(0,s)}
	*amber{|s=3|s=s.min(3); ^this.new(s,s)}
	*yellow{|s=2|s=s.min(2);^this.new(s+1,s)}
	*orange{|s=2|s=s.min(2);^this.new(s,s+1)}
	color{
		if(this.red==0){
			if(this.green==0){
				^[\black]
			}{
				^[\green,this.green];
			}
		}{
			if(this.green==0){
				^[\red,this.red];
			}{
				if(this.green==this.red){
					^[\amber,this.red];
				}{
					if(this.green>this.red){
						^[\yellow,this.red];
					}{
						^[\orange,this.green];
					}
				}
			}
		}
	}

	// next{//this method should be removed i think
	// 	var g,r;
	// 	var ns=4.collect{|g| 4.collect{|r|[g,r]}}.flatten;
	// 	#g,r=ns[(ns.indexOfEqual([green,red])+1)%ns.size];
	// 	green=g;
	// 	red=r;
	// }

	init{|gr,re,cl,co|
		green=gr;
		red=re;
		clear=cl;
		copy=co;
	}
	vel{
		^(
			(2.pow(4)*green.value.round.asInteger)
			+
			(2.pow(3)*clear)
			+
			(2.pow(2)*copy)
			+
			(red.value.round.asInteger)
		);
	}
}

LPView{
	var <value;
	var <>range;
	var <>onfront;
	var <>onhide;
	// var <>onClose;
	*new{|range,onfront,onhide|
		^super.newCopyArgs(0,range,onfront,onhide);
	}
	front{arg ... args;
		onfront.value(this,*args);
	}
	hide{arg updateLEDs=true ... args;
		range.pads.do{|p|p.remove(this,updateLEDs)};
		onhide.value(this,*args);
	}
	responseOn{
		this.subclassResponsibility(thisMethod);
	}
	responseOff{
		this.subclassResponsibility(thisMethod);
	}
	value_{|v|
		if(v!=value){
			value=v;
			this.updateLEDs;
		}
		// {
			// value.debug("no new value");
		// }
	}
	updateLEDs{
	}
}

LPButton : LPView{//this is a different lpledbutton functionality than before decide about button,switch,toggle etc...
	var <>states;
	var <>action;
	var <>mode;
	updateLEDs{
		if(states.notNil){
			range.pads.do{|p,i|p.updateLED(this,states[this.value])};
		}
	}
	*new {|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	// init{|states,action,value,mode|
	// 	this.states=states;
	// 	this.action=action;
	// 	this.mode=\forward;
	// 	this.value=value;
	// }
	responseOn{|lppad,buttonid|
		switch(mode,
			\forward,{value=(value+1)%states.size},
			\backward,{value=(value-1)%states.size},
			\stuck,{value=(value)%states.size},
			\reset,{value=0},
			{value=(value+1)%states.size});
		this.updateLEDs;
		action.value(this);
	}
	responseOff{}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|p.add(this,states[value],i)};
	}
}

LPPushButton : LPView{//this is a different lpledbutton functionality than before decide about button,switch,toggle etc...
	var <>onPushLED;
	var <>onReleaseLED;
	var <>onPushFunction;
	var <>onReleaseFunction;
	*new {|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	// init{arg onPushLED,onReleaseLED,onPushFunction,onReleaseFunction;
	// 	this.onPushLED=onPushLED?LPLED.red(1);
	// 	this.onReleaseLED=onReleaseLED?LPLED.green(1);
	// 	this.onPushFunction=onPushFunction;
	// 	this.onReleaseFunction=onReleaseFunction?{};
	// }
	responseOn{|lppad,buttonid|
		this.value=1;
		onPushFunction.value(this);
	}
	responseOff{|lppad,buttonid|
		this.value=0;
		onReleaseFunction.value(this);
	}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			p.add(this,onReleaseLED,i);
		};
	}
	updateLEDs{
		var led=if(value>0){onPushLED}{onReleaseLED};
		range.pads.do{|p,i|p.updateLED(this,led)};
	}
}

LPPushStepButton : LPView{
	var <>onPushLED;
	var <>onPushFunction;
	var <>onReleaseFunction;
	var <>states;
	var <>action;
	var <>mode;
	*new {|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	updateLEDs{
		if(states.notNil){
			range.pads.do{|p,i|p.updateLED(this,states[this.value])};
		}
	}
	responseOn{|lppad,buttonid|
		onPushFunction.value(this);
		range.pads.do{|p,i|p.updateLED(this,onPushLED)};
	}
	responseOff{|lppad,buttonid|
		onReleaseFunction.value(this);
		switch(mode,
			\forward,{value=(value+1)%states.size},
			\backward,{value=(value-1)%states.size},
			\stuck,{value=(value)%states.size},
			\reset,{value=0},
			{value=(value+1)%states.size});
		action.value(this);
		this.updateLEDs;
	}
	front{arg ... args;
		var led;
		// if(states.notNil){
		led=states[this.value];
		// }{
		// led=onReleaseLED;
		// };
		onfront.value(this,*args);
		range.pads.do{|p,i|p.add(this,states[value],i)};
	}
}

LPColorArea : LPView{
	var <>led;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	// init{|led|
	// 	this.led=led;
	// }
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			p.add(this,led,i);
		}
	}
	updateLEDs{
		range.pads.do{|p,i|p.updateLED(this,led)};
	}
}

// LP01Slider : LPView{
// 	var <> steps;
// 	var <>color;
// 	*new {|range,onfront,onhide|
// 		^super.new(range,onfront,onhide);
// 	}
// 	updateLEDs{
// 		range.pads.do{|p,i|
// 			p.updateLED(this,this.getLED(p,i));
// 		}
// 	}
// 	getLED{|p,i|
// 		var lls;
// 		var size=range.pads.size;
// 		var ledstate=value*size;
// 		if(ledstate>((size-1)-i)){
// 			lls=ledstate-((size-1)-i);
// 			ledstate=ledstate-lls;
// 			^LPLED.perform(color,lls*3);
// 		}{
// 			^LPLED.perform(background,1);
// 		}
// 	}
// }

LPSlider : LPView{
	var statevarroutine;
	var <>onoff;
	// var <value;
	var <>action;
	var <>color;
	var <>background;
	var <>incrementArray;
	var <>default;

	*new {|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	getLED{|p,i|
		var lls;
		var size=range.pads.size;
		var ledstate=value*size;
		if(ledstate>((size-1)-i)){
			lls=ledstate-((size-1)-i);
			ledstate=ledstate-lls;
			^LPLED.perform(color,lls*3);
		}{
			^LPLED.perform(background,1);
		}
	}
	updateLEDs{
		range.pads.do{|p,i|
			p.updateLED(this,this.getLED(p,i));
		}
	}
	responseOn{|lppad,buttonid|
		onoff=true;
		if(value.isNil){value=default};
		statevarroutine.stop;
		statevarroutine=r{
			while({onoff},{
				var inc=incrementArray.value[buttonid];
				if(inc.isNil){
					this.value=default.min(1).max(0);
				}{
					this.value=(value+inc).min(1).max(0);
				};
				action.value(this);
				// this.updateLEDs;
				0.2.wait;
			})
		}.play;
	}
	responseOff{|lppad,buttonid|
		onoff=false
	}
	front{arg ... args;
		var size=range.pads.size;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			p.add(this,this.getLED(p,i),i);
		};
	}
}


LPSwitchSlider : LPView{
	var <>sliderval;
	var <slidervalfunction;
	var <slideraction;
	var <>color=\red;
	var <>background=\green;
	var leds;
	var <ledfunction;
	var action;
	// var <>value;
	var <>state;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	slidervalfunction_{|slidervalfunction_|
		slidervalfunction=slidervalfunction_;
	}
	slideraction_{|slideraction_|
		slideraction=slideraction_;
		action={|l|
			if(l.state==\push){
				sliderval=slidervalfunction.value(sliderval,l.value);
				l.slideraction.value(l);
			}{
			}
		};
	}
	ledfunction_{|ledfunction_|
		ledfunction={|l|
			var lls;
			var size=range.pads.size;
			var ledstate=ledfunction_.value(l)*size;
			l.range.pads.collect{|p,i|
				if(ledstate>((size-1)-i)){
					lls=ledstate-((size-1)-i);
					ledstate=ledstate-lls;
					LPLED.perform(color,lls*3);
				}{
					LPLED.perform(background,1);
				}
			}
		}
	}
	responseOn{|lppad,buttonid|
		// var leds;
		value=buttonid;
		state=\push;
		action.value(this);
		leds=ledfunction.value(this);
		this.updateLEDs
	}
	responseOff{|lppad,buttonid|
		value=buttonid;
		state=\release;
		action.value(this);
		this.updateLEDs
	}
	front{arg ... args;
		var nix=this.onfront.value(this,*args);
		var initleds=ledfunction.value(this);
		range.pads.do{|p,i|
			p.add(this,initleds[i],i);
		}
	}
	updateLEDs{
		range.pads.do{|p,i|
			p.updateLED(this,leds[i]);
		};

		// range.pads.do{|p,i|
		// 	p.updateLED(this,this.getLED(p,i));
		// }
	}

}

LPInvisibleButtonArray : LPView{
	var <>action;
	// var <>value;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);//.init(functions,onleds,ofleds);
	}
	// init{|action|
	// 	this.action=action;
	// }
	responseOn{|lppad,buttonid|
		this.value=buttonid;
		action.value(this);
	}
	responseOff{|lppad,buttonid|
	}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			p.add(this,nil,i);
		};
	}
}

LPButtonArray : LPView{
	var <>ofleds;
	var <>onleds;
	var <>action;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	// init{|ofleds,onleds,action|
	// 	this.ofleds=ofleds;
	// 	this.onleds=onleds;
	// 	this.action=action;
	// }
	responseOn{|lppad,buttonid|
		if(value.notNil&&(value!=buttonid)){
			range.pads.asArray[value].updateLED(this,ofleds[value]);
		};
		this.value=buttonid;
		action.value(this);
	}
	responseOff{|lppad,buttonid|
	}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			var l=if(i==value){onleds[i]}{ofleds[i]};
			p.add(this,l,i);
		};
	}
	updateLEDs{
		range.pads.asArray[value].updateLED(this,onleds[value.asInteger]);
	}
}

LPFaderV : LPView{
	var <>ofled;
	var <>onled;
	var <>action;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}
	// init{|ofled,onled,action|
	// 	this.ofled=ofled;
	// 	this.onled=onled;
	// 	this.action=action;
	// }
	responseOn{|lppad,buttonid|
		this.value=buttonid;
		action.value(this);
	}
	responseOff{|lppad,buttonid|
	}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			var l=if(i<=value){onled}{ofled};
			p.add(this,l,i);
		};
	}
	updateLEDs{
		range.pads.asArray.do{|p,i|
			var l=if(i<=value){onled}{ofled};
			p.updateLED(this,l)
		}
	}
}

LPFaderH : LPView{
	var <>ofled;
	var <>onled;
	var <>action;
	*new{|range,onfront,onhide|
		^super.new(range,onfront,onhide);
	}

	responseOn{|lppad,buttonid|
		var rows=range.pads.cols;
		var cols=range.pads.rows;
		var vnr=(buttonid%rows);
		var vnc=((buttonid/rows).floor);
		this.value=vnr*cols+(vnc);
		action.value(this);
	}
	responseOff{|lppad,buttonid|
	}
	front{arg ... args;
		onfront.value(this,*args);
		range.pads.do{|p,i|
			var rows=range.pads.cols;
			var cols=range.pads.rows;
			var inr=(i%rows);
			var inc=((i/rows).floor);
			var new_i=inr*cols+(inc);
			var l=if(new_i<=value){onled}{ofled};
			p.add(this,l,i);
		};
	}
	updateLEDs{
		range.pads.asArray.do{|p,i|
			var rows=range.pads.cols;
			var cols=range.pads.rows;
			var inr=(i%rows);
			var inc=((i/rows).floor);
			var new_i=inr*cols+(inc);
			var l=if(new_i<=value){onled}{ofled};
			p.updateLED(this,l)
		}
	}
}

