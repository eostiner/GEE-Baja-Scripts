// === Section 1: Define the Region of Interest (Baja Peninsula) ===
var gadm = ee.FeatureCollection('FAO/GAUL/2015/level1');
var bajaPeninsula = gadm.filter(
  ee.Filter.and(
    ee.Filter.eq('ADM0_NAME', 'Mexico'),
    ee.Filter.inList('ADM1_NAME', ['Baja California', 'Baja California Sur'])
  )
);
var bajaPeninsulaGeometry = bajaPeninsula.geometry();
Map.centerObject(bajaPeninsulaGeometry, 6);

// Optional: Add a border layer for reference (transparent fill)
var borderStyle = { color: 'black', width: 1, fillColor: '00000000' };
Map.addLayer(bajaPeninsulaGeometry, borderStyle, 'Baja Peninsula Border');

// === Section 2: Load and Process MODIS EVI Area Data ===
var modisCollection = ee.ImageCollection('MODIS/061/MCD12Q2')
  .select('EVI_Area_1')
  .filterBounds(bajaPeninsulaGeometry)
  .filterDate('2001-01-01', '2020-12-31');

var meanEVIArea = modisCollection.mean().toFloat();

// === Section 3: Compute Dynamic Range (Client-Side Evaluation) ===
var stats = meanEVIArea.reduceRegion({
  reducer: ee.Reducer.minMax(),
  geometry: bajaPeninsulaGeometry,
  scale: 500, // MODIS MCD12Q2 resolution ~500m
  maxPixels: 1e13
});

// Evaluate the statistics on the client side.
stats.evaluate(function(result) {
  var minVal = result['EVI_Area_1_min'];
  var maxVal = result['EVI_Area_1_max'];
  
  // Provide fallback defaults if necessary.
  if (minVal === null) { minVal = 0; }
  if (maxVal === null) { maxVal = 0.1; }
  
  // Ensure a valid dynamic range.
  if (minVal === maxVal) {
    maxVal = minVal + 0.1;
  } else {
    maxVal = maxVal * 1.1;  // Add a 10% buffer to avoid clipping.
  }
  
  // === Section 4: Define Visualization Parameters ===
  var visParams = {
    min: minVal,
    max: maxVal,
    palette: ['#FFFFFF', '#FEE08B', '#FDAE61', '#F46D43', '#D73027', '#A50026'],
    opacity: 0.7
  };
  
  // === Section 5: Add Layers to the Map ===
  Map.addLayer(meanEVIArea.clip(bajaPeninsulaGeometry), visParams, 'Mean EVI Area (Clipped)', true);
  Map.addLayer(meanEVIArea, visParams, 'Mean EVI Area (Unclipped)', false);
  
  // === Section 6: Create and Add a Legend ===
  var legend = ui.Panel({
    style: {
      position: 'bottom-left',
      padding: '10px',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      border: '1px solid black'
    }
  });
  
  var legendTitle = ui.Label({
    value: 'Mean EVI Area\nMODIS 2001-2020',
    style: {
      fontWeight: 'bold',
      fontSize: '18px',
      margin: '0 0 10px 0',
      color: '#333333',
      whiteSpace: 'pre' // Allows newline characters
    }
  });
  legend.add(legendTitle);
  
  // Calculate the step for legend entries.
  var palette = visParams.palette;
  var step = (maxVal - minVal) / (palette.length - 1);
  if (step <= 0) { step = 0.1; }
  
  // Function to create a legend entry.
  var makeLegendEntry = function(color, value) {
    var colorBox = ui.Label({
      style: {
        backgroundColor: color,
        padding: '12px',
        margin: '2px 0',
        border: '1px solid #333333'
      }
    });
    var description = ui.Label({
      value: value.toFixed(2),
      style: {
        fontSize: '14px',
        margin: '0 0 2px 8px',
        color: '#333333'
      }
    });
    return ui.Panel({
      widgets: [colorBox, description],
      layout: ui.Panel.Layout.Flow('horizontal')
    });
  };
  
  // Add a legend entry for each color.
  for (var i = 0; i < palette.length; i++) {
    var value = minVal + step * i;
    legend.add(makeLegendEntry(palette[i], value));
  }
  
  Map.add(legend);
  
  // === Section 7: Optional Export ===
  Export.image.toDrive({
    image: meanEVIArea.clip(bajaPeninsulaGeometry),
    description: 'Baja_Peninsula_Mean_EVI_Area',
    folder: 'GEE_Exports',
    scale: 500,
    region: bajaPeninsulaGeometry,
    maxPixels: 1e13
  });
});
