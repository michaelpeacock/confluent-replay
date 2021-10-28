const progressBar = document.querySelector('#progress-bar'); // element where progress bar appears
const pPause = document.querySelector('#play-pause'); // element where play and pause image appears
const replayDate = document.querySelector('.replayDate'); // element where track artist appears
const replayTime = document.querySelector('.replayTime'); // element where track artist appears
const replaySpeed = document.querySelector('.replaySpeed'); // element where track artist appears

const selectTopic = document.querySelector('#select-topic');
const selectStartDate = document.querySelector('#select-start-date');
const selectStartTime = document.querySelector('#select-start-time');
const selectEndDate = document.querySelector('#select-end-date');
const selectEndTime = document.querySelector('#select-end-time');
const submit = document.querySelector('#submit');

const startTime = document.querySelector('#start-time');
const quarterTime = document.querySelector('#quarter-time');
const midTime = document.querySelector('#mid-time');
const threeQuarterTime = document.querySelector('#three-quarter-time');
const endTime = document.querySelector('#end-time');

let speeds = [1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024];
let playing = true;
let currentTime = 0;
let startTimeMS = 0;
let endTimeMS = 0;

submit.disabled = true;

addEventListener('server-connected', () => initialize());

function initialize() {
    console.log("in initialize connected " + getConnectedStatus());

    if (getConnectedStatus()) {
        console.log("subscribing to replay-data");
        stompClient.subscribe('/topic/replay-data', function (data) {
            handleReplayData(JSON.parse(data.body));
        });
    } else {
        console.log("setting timeout");
        setTimeout(initialize, 5000);
    }
}

function handleReplayData(replayData) {
    replayData.replayDataList.forEach(data => {
        console.log("time: " + convertEpochDateTime(data.timestamp));
        console.log("key: " + data.key);
        console.log("value: " + data.value);

        replayDate.innerHTML = convertEpochDate(data.timestamp);
        replayTime.innerHTML = convertEpochTime(data.timestamp);

        let progressValue = ((data.timestamp - startTimeMS) / (endTimeMS - startTimeMS)) * 100;
        console.log("progress value: " + progressValue);
        progressBar.value = progressValue;

        if (data.key || data.value) {
            var table = document.getElementById("replay-data-table");
            var row = table.insertRow(1);
            var timeCell = row.insertCell(0);
            var dataCell = row.insertCell(1);
            timeCell.style = "width:150px";
            timeCell.innerHTML = convertEpochDateTime(data.timestamp);
            dataCell.innerHTML = "key: " + data.key + " - value: " + data.value;
        }
    });

}



function playPause() {
    if (playing) {
        pPause.src = "./assets/icons/pause.png"
        playing = false;
        sendUpdate("/setState", "PLAY");
    } else {
        pPause.src = "./assets/icons/play.png"
        playing = true;
        sendUpdate("/setState", "PAUSE");
    }
}

function stop() {
    pPause.src = "./assets/icons/play.png"
    playing = true;
    sendUpdate("/setState", "STOP");

}

function verifySubmit() {
    if (selectTopic.value.length > 0 &&
        selectStartDate.value.length > 0 &&
        selectStartTime.value.length > 0 &&
        selectEndDate.value.length > 0 &&
        selectEndTime.value.length > 0) {
        submit.disabled = false;
    } else {
        submit.disabled = true;
    }
}

function fastForward() {
    let nextSpeed = 0;
    switch (replaySpeed.innerHTML) {
        case '1x':
            nextSpeed = 1;
            break;
        case '2x':
            nextSpeed = 2;
            break;
        case '4x':
            nextSpeed = 3;
            break;
        case '8x':
            nextSpeed = 4;
            break;
        case '16x':
            nextSpeed = 5;
            break;
        case '32x':
            nextSpeed = 6;
            break;
        case '64x':
            nextSpeed = 7;
            break;
        case '128x':
            nextSpeed = 8;
            break;
        case '256x':
            nextSpeed = 9;
            break;
        case '512x':
            nextSpeed = 10;
            break;
    }

    sendUpdate("/setSpeed/", speeds[nextSpeed]);
    replaySpeed.innerHTML = speeds[nextSpeed] + 'x';

}

function sendUpdate(url, value) {
    const request = new XMLHttpRequest();
    request.open('POST', url, true);
    request.setRequestHeader("Accept", "application/json");
    request.setRequestHeader("Content-Type", "application/json");

    request.onreadystatechange = function() { // Call a function when the state changes.
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            // Request finished. Do processing here.
        }
    }

    console.log("sending: " + value);
    request.send(value);

}

function formatTime(seconds) {
    let min = Math.floor((seconds / 60));
    let sec = Math.floor(seconds - (min * 60));
    if (sec < 10){ 
        sec  = `0${sec}`;
    };
    return `${min}:${sec}`;
};


function updateProgressValue() {
    progressBar.value = currentTime++;
};

//setInterval(updateProgressValue, 500);

function changeProgressBar() {
    let deltaTime = endTimeMS - startTimeMS;
    console.log("progress bar value: " + progressBar.value);
    currentTime = progressBar.value;
    sendUpdate("/setTime/", startTimeMS + (deltaTime * (progressBar.value/100)));

};

function submitReplayConfig() {
    const startTS = selectStartDate.value + "T" + selectStartTime.value + ":00.00-0400";
    startTimeMS = ((new Date(startTS)).getTime()) // / 1000;

    const endTS = selectEndDate.value + "T" + selectEndTime.value + ":00.00-0400";
    endTimeMS = ((new Date(endTS)).getTime()) /// 1000;

    let configData = {
        "topic": selectTopic.value,
        "startTime": startTimeMS,
        "endTime": endTimeMS
    };

    const url = '/setConfigValues/';
    const request = new XMLHttpRequest();
    request.open('POST', url, true);
    request.setRequestHeader("Accept", "application/json");
    request.setRequestHeader("Content-Type", "application/json");

    request.onreadystatechange = function() { // Call a function when the state changes.
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            // Request finished. Do processing here.
            updateProgressTimes(startTimeMS, endTimeMS);
            replayDate.innerHTML = convertEpochDate(startTimeMS);
            replayTime.innerHTML = convertEpochTime(startTimeMS);
        }
    }

    console.log("sending " + JSON.stringify(configData));
    request.send(JSON.stringify(configData));

//    updateProgressTimes(startTime, endTime);
//    replayDate.innerHTML = convertEpochDate(startTimeMS);
//    replayTime.innerHTML = convertEpochTime(startTimeMS);

}

function updateProgressTimes(sTime, eTime) {
    let deltaTime = eTime - sTime;

    startTime.innerHTML = convertEpochDateTime(sTime);
    quarterTime.innerHTML = convertEpochDateTime(sTime + (deltaTime*0.25));
    midTime.innerHTML = convertEpochDateTime(sTime + (deltaTime*0.5));
    threeQuarterTime.innerHTML = convertEpochDateTime(sTime + (deltaTime*0.75));
    endTime.innerHTML = convertEpochDateTime(eTime);
}

function convertEpochDateTime(epochTime) {
    return convertEpochDate(epochTime) + " " + convertEpochTime(epochTime);
}

function convertEpochDate(epochTime) {
    var time = new Date(epochTime);

    let formatTime;
    formatTime = (time.getMonth() + 1).toString().padStart(2,'0');
    formatTime += "/";
    formatTime += time.getDate().toString().padStart(2,'0');
    formatTime += "/";
    formatTime += time.getFullYear();

    return formatTime;
}

function convertEpochTime(epochTime) {
    var time = new Date(epochTime);

    let formatTime;
    formatTime = time.getHours().toString().padStart(2,'0');
    formatTime += ":";
    formatTime += time.getMinutes().toString().padStart(2,'0');
    formatTime += ":";
    formatTime += time.getSeconds().toString().padStart(2,'0');

    return formatTime;
}