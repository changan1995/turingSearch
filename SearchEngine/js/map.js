function initMap() {
  var uluru = {lng: -75.1652215, lat: 39.9525839};
  var map = new google.maps.Map(document.getElementById('map'), {
    zoom: 4,
    center: uluru
  });
  var marker = new google.maps.Marker({
    position: uluru,
    map: map
  });
}