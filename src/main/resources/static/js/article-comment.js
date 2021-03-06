
var token = $('#_csrf').attr('content');
var header = $('#_csrf_header').attr('content');
var isAdmin = $('#isAdmin').attr('content');
var articleId = $('#articleId').attr('content');
var currentUsername = $('#currentUsername').attr('content');

var stompClient = null;

connect();

function connect() {
    var socket = new SockJS('/comment-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/comments/' + articleId, function (comment) {
            addComment(JSON.parse(comment.body));
        });
    });
}

$('#btn-comment-add').click(function() {
    if (!$.trim($('#textarea-comment').val())) return;

    $(this).prop('disabled', true);

    var comment = {
        text: $("#textarea-comment").val(),
        articleId: articleId
    }

    ajaxSaveComment(comment);
});

function ajaxSaveComment(comment) {
    $.ajax({
        type: 'POST',
        url: '/comment/add',
        data: JSON.stringify(comment),
        contentType: 'application/json; charset=utf-8',
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        },
        success : function() {
            console.log("Comment was added successfully");

            $('#textarea-comment').html("");
            $('#btn-comment-add').prop('disabled', false);
        },
        error : function(e) {
            alert("Comment adding error");
            console.log("ERROR: ", e);

            $('#textarea-comment').html("");
            $('#btn-comment-add').prop('disabled', false);
        }
    });
}
var deletedCommentId;
function delComment(commentId) {
    deletedCommentId = commentId;
    $.ajax({
        type: 'DELETE',
        url: '/comment/delete',
        data: JSON.stringify(commentId),
        contentType: 'application/json; charset=utf-8',
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        },
        success : function() {
            $('#comment-' + deletedCommentId).append('' +
                '<div class="col-md-6">' +
                '  <span style="color: #FF0000">' + LANG.comment_deleted + '</span>' +
                '</div>');
            $('#btn-edit-' + deletedCommentId).empty();
            $('#btn-del-' + deletedCommentId).empty();
            console.log("Comment was deleted successfully");
        },
        error : function(e) {
            alert("Comment deletion error");
            console.log("ERROR: ", e);
        }
    });
}

function addComment(comment) {
    var commentHtml = '\n\n' +
        '            <div class="row" id="comment-' + comment.id + '">' +
        '                <div class="col-md-6 border border-left-0 border-right-0">\n' +
        '                    <div class="row mt-1">\n' +
        '                        <div class="col-md-5">\n' +
        '                            <span><b>by</b>\n' +
        '                                <a id="comment-author-' + comment.id + '" href="/user/' + comment.author + '" class="font-weight-bold">' + comment.author + '</a>\n' +
        '                            </span>\n' +
        '                        </div>\n' +
        '                        <span id="comment-timestamp-' + comment.timestamp + '" class="col-md-5">' + comment.timestamp + '</span>\n' +
        '                        <div class="text-right">\n';
    if (comment.author == currentUsername) {
        commentHtml += '' +
        '                            <span onclick="editComment(' + comment.id + ')" class="clickable-icon" id="btn-edit-' + comment.id + '">\n' +
        '                                <i class="fas fa-edit"></i>\n' +
        '                            </span>\n';
    }

    if (comment.author == currentUsername || isAdmin == 'true') {
        commentHtml += '' +
        '                            <span \n' +
        '                                      onclick="delComment(' + comment.id + ')" class="clickable-icon" id="btn-del-' + comment.id + '"\n' +
        '                                      value="' + comment.id + '">\n' +
        '                                <i class="fas fa-trash-alt  mr-2 mt-2"></i>\n' +
        '                            </span>\n';
    }
    commentHtml += '' +
        '                        </div>\n' +
        '                    </div>\n' +
        '                    <div class="pl-3 row">\n' +
        '                        <p class="mt-1" id="comment-text-' + comment.id + '">' + comment.text + '</p>\n' +
        '                    </div>\n' +
        '                </div>' +
        '              </div>';
    $("#comments").append(commentHtml);

    document.getElementById('textarea-comment').value = "";
}
