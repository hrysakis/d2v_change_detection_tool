$(document).ready(function() {
    // alert('i am ready!');
    var options = {
        beforeSend: function() {
            $("#progressbox").show();
            // clear everything
            $("#progressbar").width('0%');
            $("#message").empty();
            $("#percent").html("0%");
        },
        uploadProgress: function(event, position, total, percentComplete) {
            $("#progressbar").width(percentComplete + '%');
            $("#percent").html(percentComplete + '%');

            // change message text and % to red after 50%
            if (percentComplete > 50) {
                $("#message").html("<font color='red'>File Upload is in progress .. </font>");
            }
        },
        success: function() {
            $("#progressbar").width('100%');
            $("#percent").html('100%');
        },
        complete: function(response) {
            $("#message").html("<font color='green'>Dataset version has been uploaded! </font>");
            //If dlabel element exists i am in add dataset/version mode else i am in add only version
            if ( $( "#myDiv" ).length ){
                $("#message").append("<font color='green'>You can optionally upload a custom configation file below. </font>");
            }
            $("#message").append("<font color='green'>Finally press 'Add' to proceed.</font>");
        },
        error: function() {
            $("#message").html("<font color='red'> ERROR: unable to upload files</font>");
        }
    };
    $("#UploadForm").ajaxForm(options);
});