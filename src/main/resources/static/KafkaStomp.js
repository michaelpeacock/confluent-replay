var stompClient = null;
var isConnected = false;
var socket = null;
connect();

function setConnected(connected) {
    //$("#dashboard-data").html("");
    isConnected = connected;
}

function getConnectedStatus() {
    return isConnected;
}

function connect() {
    socket = new SockJS('/confluent-websocket');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        setConnected(true);
        dispatchEvent(new CustomEvent("server-connected"));
        console.log('Connected: ' + frame);
    });

    socket.onclose = function() {
        // connection closed, discard old websocket and create a new one in 5s
        disconnect();
        socket = null;
        setTimeout(connect, 5000);
    };
}



function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
        stompClient = null;
    }
    setConnected(false);
    console.log("Disconnected");
}

function handleDNSData(dns_data) {
    //console.log("message: " + dns);
    if (dnsTableHeaderCreated == false) {
        createDNSTableHeader(dns_data[0]);
    }
    var table = document.getElementById("dns-data-table");
    for (var i = 0; i < dns_data.length; i++) {
        var tr = table.insertRow(1);
        for (var j = 0; j < dnsCols.length; j++) {
            var tabCell = tr.insertCell(-1);
            if (dnsCols[j] == "ts") {
                const event = new Date(dns_data[i][dnsCols[j]]);
                tabCell.innerHTML = event.getHours() + ":" + event.getMinutes() + ":" + event.getSeconds() + ":" + event.getMilliseconds();
            } else {
                tabCell.innerHTML = dns_data[i][dnsCols[j]];
            }
        }
    }

    updateDataCounter(dns_data.length);
}

