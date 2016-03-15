$(document).ready(function() {
    // alert('i am ready!');
    var options = {
        beforeSend: function() {
            $("#progressbox2").show();
            // clear everything
            /*$("#progressbar2").width('0%');*/
            $("#message2").empty();
            //$("#percent2").html("0%");
        },
        uploadProgress: function(event, position, total, percentComplete) {
            $("#progressbar2").width(percentComplete + '%');
            //$("#percent2").html(percentComplete + '%');

            // change message text and % to red after 50%
            /*if (percentComplete > 50) {
                $("#message2").html("<font color='red'>File Upload is in progress .. </font>");
            }*/
        },
        success: function() {
            /*$("#progressbar2").width('100%');
            $("#percent2").html('100%');*/
        },
        complete: function(response) {
            
            $("#message2").html("<font color='green'>Your JSON file has been uploaded! Click 'Save' to proceed.</font>");
            //parseJSON
            
            var jsonfilename = document.getElementById("myfile2").files[0].name;
          
             $.get('ActionServlet', {action: "parseJSON", jsonfilename:jsonfilename}).done(function(responseText) {
             $('#jsonstring').val(responseText);
             });
                 
        },
        error: function() {
            $("#message2").html("<font color='red'> ERROR: unable to upload files</font>");
        }
    };
    $("#UploadForm2").ajaxForm(options);
});