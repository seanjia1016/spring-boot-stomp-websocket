let stompClient = null;

const showMessage = (content) => {
    console.log("接到訊息：" + content)
    $("#showMessage").append("<tr><td>" + content + "</td></tr>")
}

const connect = () => {
    const socket = new SockJS("our-websocket");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, (frame) => {
        console.log("連接：" + frame);
        // 公共訊息通過（訂閱 /topic/chat，與 RedisMessageListener 轉發的頻道一致）
        stompClient.subscribe("/topic/chat", (message) => {
            showMessage(JSON.parse(message.body).content)
        })
        // 私信通過——前加 user
        stompClient.subscribe("/user/topic/privateMessage", (message) => {
            showMessage(JSON.parse(message.body).content)
        })
    })
}

const sendMessage = () => {
    console.log("發送訊息：")
    stompClient.send("ws/message", {}, JSON.stringify({"content": $("#sendMessage").val()}))
}

const sendPrivateMessage = () => {
    console.log("發送私訊：")
    stompClient.send("ws/privateMessage", {}, JSON.stringify({"content": $("#sendPrivateMessage").val()}))
}

$(document).ready(() => {
    console.log("index 頁面準備完畢……")
    connect();
    $("#send").click(() => {
        sendMessage();
    })
    $("#sendPrivate").click(() => {
        sendPrivateMessage();
    })
})
