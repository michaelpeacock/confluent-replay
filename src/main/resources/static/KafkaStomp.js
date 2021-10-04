var stompClient = null;
let isConnected = false;
connect();

function setConnected(connected) {
    //$("#dashboard-data").html("");
    isConnected = connected;
}

function getConnectedStatus() {
    return isConnected;
}

function connect() {
    var socket = new SockJS('/confluent-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
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

