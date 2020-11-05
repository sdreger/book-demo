var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#offer-table").show();
        $("#bid-table").show();
    }
    else {
        $("#offer-table").hide();
        $("#bid-table").hide();
    }
    $("#offer-body").html("");
    $("#bid-body").html("");
}

function connect() {
    var socket = new SockJS('/gs-guide-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/bookUpdate', function (payload) {
            updateBook(JSON.parse(payload.body));
        });
    });
    $("#start-pooling").prop("disabled", false);
}

function disconnect() {
    stopPooling();
    $("#start-pooling").prop("disabled", true);
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");

}

function startPooling() {
    stompClient.send("/app/startPooling", {}, JSON.stringify({'listSize': $("#list-size").val()}));
    $("#start-pooling").prop("disabled", true);
    $("#stop-pooling").prop("disabled", false);
}

function stopPooling() {
    stompClient.send("/app/stopPooling", {}, {});
    $("#start-pooling").prop("disabled", false);
    $("#stop-pooling").prop("disabled", true);
}

function updateBook(message) {
  $("#bid-body").html("");
  for (const [key, value] of Object.entries(message.bids)) {
    $("#bid-body").append("<tr><td>" + key + "</td><td>" + value + "</td><td>" + (key * value).toFixed(5) + "</td></tr>");
  }
  $("#offer-body").html("");
  for (const [key, value] of Object.entries(message.offers)) {
    $("#offer-body").append("<tr><td>" + key + "</td><td>" + value + "</td><td>" + (key * value).toFixed(5) + "</td></tr>");
  }
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#start-pooling" ).click(function() { startPooling(); });
    $( "#stop-pooling" ).click(function() { stopPooling(); });
});
