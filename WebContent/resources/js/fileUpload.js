function handleFileSelectEvent(evt) {
	
	var file = evt.target.files[0];
	//alert (file.name);
	
	// Only process image files.
    //if (!file.type.match('image.*')) {
    //  continue;
    //}
	
    var reader = new FileReader();
    var senddata = new Object();
    /*
    senddata.name = file.name;
    senddata.date = file.lastModified;
    senddata.size = file.size;
    senddata.type = file.type;
    */
    
    // Wenn der Dateiinhalt ausgelesen wurde...
    reader.onload = function(theFileData) {
      senddata.fileData = theFileData.target.result; // Ergebnis vom FileReader auslesen
      
      // server upload as base64
      uploadFile([{name:'param1', value:senddata.fileData}]);
    }

    reader.readAsDataURL(file);
}

function jsfileUpload() {
	var f=document.createElement('input');f.style.display='none';f.type='file';
	f.name='file';document.getElementById('form').appendChild(f);
	f.addEventListener("change", handleFileSelectEvent,"false");
	f.click();
}