/*global navigator, AudioContext, Uint8Array*/
(function (exports) {
  /*
   CORPUS is the array which we match the user input against. The number of notes to be played
   is simply the number of BEATS * MEASURES (bars).

   For this level our settings are:

   60 BPM (1 second per note)
   3/4 time signature
   2 measures

   Happy playing!
   */

  //var CORPUS = [{note: "D", pos: 0}, {note: "E", pos: 1}, {note: "F", pos: 2}, 0, 0];
  var CORPUS = [0, 0, 0, 0, 0, 0];
  var NOTE_STRINGS = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];
  var BEATS = 3;
  var MEASURES = 2;
  var NOTES = BEATS * MEASURES;
  var audioContext = new AudioContext();

  function getUserMedia(dictionary, callback) {
    try {
      navigator.getUserMedia = navigator.getUserMedia ||
        navigator.webkitGetUserMedia ||
        navigator.mozGetUserMedia;
      navigator.getUserMedia(dictionary, callback, function (err) {
        console.log('Stream generation failed: ', err.message);
      });
    } catch (err) {
      console.log('getUserMedia throw exception: ', err.message);
    }
  }

  function requestAnimFrame(callback) {
    window.requestAnimFrame = function (callback) {
      window.setTimeout(callback, 1000);
    };

    return window.requestAnimFrame(callback);
  }

  function noteFromPitch(pitch) {
    var noteNum = 12 * (Math.log(pitch / 440) / Math.log(2));
    return Math.round(noteNum) + 69;
  }

  function frequencyFromNoteNumber(note) {
    return 440 * Math.pow(2, (note - 69) / 12);
  }

  function centsOffFromPitch(frequency, note) {
    return Math.floor(1200 * Math.log(frequency / frequencyFromNoteNumber(note)) / Math.log(2));
  }

  function autoCorrelate(buf, sampleRate) {
    var MIN_SAMPLES = 4;        // corresponds to an 11kHz signal
    var MAX_SAMPLES = 1000; // corresponds to a 44Hz signal
    var SIZE = 1000;
    var best_offset = -1;
    var best_correlation = 0;
    var rms = 0;

    if (buf.length < (SIZE + MAX_SAMPLES - MIN_SAMPLES)) {
      return -1;  // Not enough data
    }

    for (var i = 0; i < SIZE; i++) {
      var val = (buf[i] - 128) / 128;
      rms += val * val;
    }
    rms = Math.sqrt(rms / SIZE);

    for (var offset = MIN_SAMPLES; offset <= MAX_SAMPLES; offset++) {
      var correlation = 0;

      for (var i = 0; i < SIZE; i++) {
        correlation += Math.abs(((buf[i] - 128) / 128) -
          ((buf[i + offset] - 128) / 128));
      }
      correlation = 1 - (correlation / SIZE);
      if (correlation > best_correlation) {
        best_correlation = correlation;
        best_offset = offset;
      }
    }
    if ((rms > 0.01) && (best_correlation > 0.01)) {
      // console.log("f = " + sampleRate/best_offset + "Hz (rms: " + rms + " confidence: " + best_correlation + ")")
      return sampleRate / best_offset;
    }
    return -1;
    //        var best_frequency = sampleRate/best_offset;
  }

  function Detector() {
    this.pitch;
    this.note;
    this.notes = [];
    this.cycle = -1;
    this.noteString = '';
    this.detune = 0;
    this.analyser = null;
    this.buflen = 2048;
    this.buf = new Uint8Array(this.buflen);
    this.requestId = null;
    this.audioStream = null;
  }

  Detector.prototype.startLiveInput = function () {
    getUserMedia({audio: true}, this.gotStream.bind(this));
  };

  Detector.prototype.gotStream = function (stream) {
    // Create an AudioNode from the stream.
    this.audioStream = stream;
    var mediaStreamSource = audioContext.createMediaStreamSource(stream);

    // Connect it to the destination.
    this.analyser = audioContext.createAnalyser();
    this.analyser.fftSize = 2048;
    mediaStreamSource.connect(this.analyser);

    window.setTimeout(this.updatePitch.bind(this), 1000);
  };

  Detector.prototype.updatePitch = function () {
    this.cycle += 1;
    if (this.cycle < NOTES) {
      this.analyser.getByteTimeDomainData(this.buf);

      var ac = autoCorrelate(this.buf, audioContext.sampleRate);
      if (ac !== -1) {
        this.pitch = ac;
        this.note = noteFromPitch(ac);
        var obj = {note: NOTE_STRINGS[this.note % 12], pos: this.cycle};
        this.notes.push(obj);
        this.detune = centsOffFromPitch(ac, this.note);
      } else {
        this.notes.push(0);
      }

      console.log(this.notes);

      requestAnimFrame(this.updatePitch.bind(this));
      var noteBoxElement = "note-" + (this.cycle + 1);
      document.getElementById(noteBoxElement).innerHTML = this.notes[this.cycle];
    } else {
      if (_.isEqual(this.notes, CORPUS)) {
        document.getElementById('result').innerHTML = 'WIN';
      } else {
        document.getElementById('result').innerHTML = 'FAILED';
      }

      return this.notes;
    }
  };

  exports.pitchDetector = new Detector();
})(this);
