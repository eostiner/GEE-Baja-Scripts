// Google Earth Engine script to analyze MODIS Land Cover across the Baja California Peninsula

// Load the MODIS MCD12Q1 land cover dataset for 2020
var modisLandCover = ee.ImageCollection("MODIS/061/MCD12Q1")
                        .filterDate('2020-01-01', '2020-12-31')
                        .select('LC_Type1')
                        .median();

// Define the Baja California Peninsula boundary from GADM dataset
var bajaBoundary = ee.FeatureCollection("FAO/GAUL/2015/level1")
                      .filter(ee.Filter.or(
                        ee.Filter.eq('ADM1_NAME', 'Baja California'),
                        ee.Filter.eq('ADM1_NAME', 'Baja California Sur')
                      ));

// Clip MODIS data to the Baja boundary
var bajaLandCover = modisLandCover.clip(bajaBoundary);

// Display MODIS land cover data on the map
var visParams = {
  min: 1, max: 17,
  palette: ['05450a', '086a10', '54a708', '78d203', '009900',
            'c6b044', 'dcd159', 'dade48', 'fbff13', 'b6ff05',
            '27ff87', 'c24f44', 'a5a5a5', 'ff6d4c', '69fff8',
            '1c0d02', 'ffffb2']
};

Map.centerObject(bajaBoundary, 6);
Map.addLayer(bajaLandCover, visParams, 'MODIS Land Cover 2020');

// Outline Baja Peninsula
Map.addLayer(bajaBoundary.style({color: 'rgba(255, 255, 255, 0)', width: 2, fillColor: '00000000'}), {}, 'Baja Peninsula Boundary');

// Add a legend to display land cover classifications
var legend = ui.Panel({
  style: {
    position: 'bottom-left',
    padding: '8px 15px'
  }
});

var legendTitle = ui.Label({
  value: 'MODIS Land Cover Classes',
  style: {fontWeight: 'bold', fontSize: '12px', margin: '0 0 4px 0'}
});
legend.add(legendTitle);

// Define land cover classes with Baja-specific descriptions (condensed, no parentheses)
var modisClasses = [
  {name: "Sierra Pine Forests – Mountain coniferous forests", color: '05450a'},
  {name: "Tropical Dry Forests – Southern Baja dry forests", color: '086a10'},
  {name: "Highland Pine-Woodlands – Isolated coniferous forests", color: '54a708'},
  {name: "Oak-Pine Transition – Mid-elevation oak forests", color: '78d203'},
  {name: "Mixed Pine-Oak Forests – High-altitude transitional woodlands", color: '009900'},
  {name: "Coastal Chaparral Shrublands – Pacific scrub vegetation", color: 'c6b044'},
  {name: "Sonoran Desert Scrub – Cacti-dominated arid lands", color: 'dcd159'},
  {name: "Mezquital Thorny Scrub – Lowland mesquite woodlands", color: 'dade48'},
  {name: "Sparse Desert Savannas – Arid semi-grasslands", color: 'fbff13'},
  {name: "Bajadas Dry Grasslands – Drought-adapted desert grasses", color: 'b6ff05'},
  {name: "Coastal Lagoons Mangroves – Wetland estuary ecosystems", color: '27ff87'},
  {name: "Irrigated Agriculture Orchards – Desert farmland areas", color: 'c24f44'},
  {name: "Urban Resort Developments – Cities and tourism zones", color: 'a5a5a5'},
  {name: "Agro-Desert Transition – Mixed land-use areas", color: 'ff6d4c'},
  {name: "Snow Ice Peaks – Sierra high-altitude snow", color: '69fff8'},
  {name: "Barren Rocky Plains – Volcanic fields rocklands", color: '1c0d02'},
  {name: "Water Bodies Pacific Gulf – Marine ecosystems", color: 'ffffb2'}
];

// Create a legend panel
var legend = ui.Panel({
  style: {
    position: 'bottom-left',
    padding: '8px 15px',
    backgroundColor: 'white' // Light transparent background
  }
});

// Add a legend title
legend.add(ui.Label({
  value: 'MODIS Land Cover - Baja',
  style: {fontWeight: 'bold', fontSize: '14px', margin: '0 0 4px 0'}
}));

// Function to add each legend entry
modisClasses.forEach(function(modisClass) {
  var colorBox = ui.Label({
    style: {
      backgroundColor: modisClass.color,
      padding: '8px',
      margin: '2px 4px 2px 0',
      width: '20px', height: '20px'
    }
  });

  var classLabel = ui.Label({
    value: modisClass.name,
    style: {margin: '2px 0', fontSize: '12px'}
  });

  var panel = ui.Panel({
    widgets: [colorBox, classLabel],
    layout: ui.Panel.Layout.Flow('horizontal')
  });

  legend.add(panel);
});

// Add the legend to the map
Map.add(legend);


// Export the MODIS land cover image to Google Drive
Export.image.toDrive({
  image: bajaLandCover,
  description: 'MODIS_LandCover_Baja',
  region: bajaBoundary.geometry(),
  scale: 500,
  crs: 'EPSG:4326',
  maxPixels: 1e13,
  fileFormat: 'GeoTIFF'
});
