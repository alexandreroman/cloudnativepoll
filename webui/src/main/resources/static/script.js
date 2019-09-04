function init() {
    var links = document.getElementsByClassName("vote");
    console.log("Initializing");
    for (var i = 0; i < links.length; ++i) {
        var link = links[i];
        link.removeAttribute("href");
    }

    setTimeout(refreshVotes, 1000);
}

function onCastVote(e) {
    var vote = e.getAttribute("vote");
    castVote(vote);
}

function castVote(vote) {
    console.log("Casting vote: " + vote);
    axios.post("/votes", {
        choice: vote
    }).then(function (response) {
        console.log("Successfully sent vote");
    }).catch(function (error) {
        console.log("Failed to cast vote: " + error);
    });
}

function refreshVotes() {
    console.log("Refreshing votes");
    axios.get("/votes", {}).then(function (response) {
        console.log("Received vote results");
        var results = document.getElementsByClassName("result");
        for (var i = 0; i < results.length; ++i) {
            var result = results[i];
            var vote = result.getAttribute("vote");
            if (vote in response.data) {
                var voteValue = response.data[vote];
                console.log("Vote: " + vote + "=" + voteValue);
                result.innerHTML = voteValue;
            }
        }
        setTimeout(refreshVotes, 1000);
    }).catch(function (error) {
        console.log("Failed to get vote results: " + error);
        setTimeout(refreshVotes, 1000);
    });
}
