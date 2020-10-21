# Launchpad

Supercollider extension to develop a Novation Launchpad based interfaces similar to a GUI. The Interface is designed around the concept that you like to make your launchpad behave like a GUI with buttons and sliders. These widgets can be layered.

The library contains the following widgets:

LPButton,
LPButtonArray,
LPPushButton,
LPPushStepButton,
LPSlider,
LPFaderH,
LPFaderV,
LPColorArea,
LPInvisibleButtonArray

## Installation

Use the Supercollider Quarks GUI:

```supercollider
Quarks.gui
```

## Examples

Example of creating a button on the launchpad:

```supercollider
Launchpad.initMIDI; // before the Launchpad can be used it has to be initialized
(
a = LPButton(Launchpad.getRange(0,0,0,0),{"Do something when the button is put to the front of the Launchpad.".postln},{"Do something when it is hidden from the Launchpad.".postln});
a.states = [LPLED.green,LPLED.red,LPLED.yellow];
a.action = {|b| "Print Current state:".post; b.value.postln; };
a.front;
)
a.hide;// to hide the gui element
```

Example of a Slider:

```supercollider
a=LPSlider(Launchpad.getRange(0,1,0,7));
a.incrementArray=[1/80,1/160,1/400,nil,1/ -400,1/ -160,1/ -80];
a.color=\amber;
a.background=\green;
a.action={|s|s.value.debug(\slidervalue)};
a.default = 0.6
a.front
```

## Change log

- Thanks to @LiveTiles to fix the results of my forgetfulness.

## License

GNU GENERAL PUBLIC LICENSE Version 3
