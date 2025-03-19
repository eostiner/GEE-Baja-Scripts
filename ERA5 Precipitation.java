// Load the GADM dataset for administrative boundaries (level 1 corresponds to states/provinces)
var gadm = ee.FeatureCollection('FAO/GAUL/2015/level1');

// Filter the GADM dataset to include only Baja California and Baja California Sur
var bajaPeninsula = gadm.filter(
  ee.Filter.and(
    ee.Filter.eq('ADM0_NAME', 'Mexico'),
    ee.Filter.inList('ADM1_NAME', ['Baja California', 'Baja California Sur'])
  )
);

// Merge the two states into a single geometry for clipping
var bajaPeninsulaGeometry = bajaPeninsula.geometry();

// Center the map on the Baja Peninsula
Map.centerObject(bajaPeninsulaGeometry, 6);

// Load the ERA5 Monthly dataset and select the total precipitation band
var era5Monthly = ee.ImageCollection('ECMWF/ERA5/MONTHLY')
  .select('total_precipitation');

// Filter the dataset to the Baja Peninsula (optional, speeds up processing)
var era5Filtered = era5Monthly.filterBounds(bajaPeninsulaGeometry);

// Calculate the mean total precipitation over the entire time period
// (Note: If you want a specific time period, add .filterDate('YYYY-MM-DD', 'YYYY-MM-DD') before .mean())
var meanPrecipitation = era5Filtered.mean();

// Convert precipitation from meters to millimeters (multiply by 1000)
var meanPrecipitationMM = meanPrecipitation.multiply(1000);

// Define visualization parameters for the heatmap
var visParams = {
  min: 0,           // Minimum precipitation value in mm
  max: 100,         // Maximum precipitation value in mm (adjust based on your data)
  palette: ['#FFFFFF', '#00FFFF', '#0080FF', '#DA00FF', '#FFA400', '#FF0000'],
  opacity: .8
};

// Add the heatmap layer to the map, clipped to the Baja Peninsula
Map.addLayer(meanPrecipitationMM.clip(bajaPeninsulaGeometry), visParams, 'Mean Total Precipitation (mm)');



// Create a legend (descriptive key) for the heatmap
var legend = ui.Panel({
  style: {
    position: 'bottom-left',
    padding: '8px 15px'
  }
});

// Add a title to the legend
var legendTitle = ui.Label({
  value: 'Mean Total Precipitation (mm)\nERA5 - 1979-2025',
  style: {
    whiteSpace: 'pre', // or 'pre-wrap'
    fontWeight: 'bold',
    fontSize: '16px',
    margin: '0 0 4px 0'
  }
});
legend.add(legendTitle);

// Define the color palette and corresponding precipitation values
var palette = visParams.palette;
var minValue = visParams.min;
var maxValue = visParams.max;
var step = (maxValue - minValue) / (palette.length - 1);

// Function to create legend entries
var makeLegendEntry = function(color, value) {
  var colorBox = ui.Label({
    style: {
      backgroundColor: color,
      padding: '8px',
      margin: '0 0 4px 0'
    }
  });
  var description = ui.Label({
    value: value.toFixed(0) + ' mm',
    style: {margin: '0 0 4px 4px'}
  });
  return ui.Panel({
    widgets: [colorBox, description],
    layout: ui.Panel.Layout.Flow('horizontal')
  });
};

// Add legend entries for each color in the palette
for (var i = 0; i < palette.length; i++) {
  var value = minValue + i * step;
  legend.add(makeLegendEntry(palette[i], value));
}

// Add the legend to the map
Map.add(legend);

// Optional: Export the heatmap to Google Drive (uncomment to use)
// Export.image.toDrive({
//   image: meanPrecipitationMM.clip(bajaPeninsulaGeometry),
//   description: 'Baja_Peninsula_Mean_Precipitation',
//   folder: 'GEE_Exports',
//   scale: 27830, // ERA5 native resolution in meters (approximately 31 km)
//   region: bajaPeninsulaGeometry,
//   maxPixels: 1e13
// });