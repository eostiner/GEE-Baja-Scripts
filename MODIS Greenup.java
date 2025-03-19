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

// === Section 2: Load and Process MODIS Greenup Data ===
var modisCollection = ee.ImageCollection('MODIS/061/MCD12Q2')
  .select('Greenup_1')
  .filterBounds(bajaPeninsulaGeometry)
  .filterDate('2001-01-01', '2020-12-31');

var meanGreenup = modisCollection.mean().toFloat();

// === Section 3: Compute Dynamic Range (Client-Side Evaluation) ===
var stats = meanGreenup.reduceRegion({
  reducer: ee.Reducer.minMax(),
  geometry: bajaPeninsulaGeometry,
  scale: 500, // Approximate resolution (~500m)
  maxPixels: 1e13
});

stats.evaluate(function(result) {
  var minVal = result['Greenup_1_min'];
  var maxVal = result['Greenup_1_max'];

  // Fallback defaults if necessary.
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
    palette: ['#FFFFB2', '#FECC5C', '#FD8D3C', '#F03B20', '#BD0026'],
    opacity: 0.8
  };

  // === Section 5: Add Layers to the Map ===
  Map.addLayer(meanGreenup.clip(bajaPeninsulaGeometry), visParams, 'Mean Greenup (Clipped)', true);
  Map.addLayer(meanGreenup, visParams, 'Mean Greenup (Unclipped)', false);

  // === Section 6: Create a Combined Legend with Interpretation ===
  var combinedLegend = ui.Panel({
    style: {
      position: 'bottom-left',
      padding: '10px',
      backgroundColor: 'rgba(255,255,255,0.9)',
      border: '1px solid black'
    }
  });
  
  // Legend title.
  combinedLegend.add(ui.Label({
    value: 'Mean Greenup\nMODIS 2001-2020',
    style: { fontWeight: 'bold', fontSize: '16px', margin: '0 0 10px 0', whiteSpace: 'pre' }
  }));
  
  // Calculate step based on dynamic range.
  var palette = visParams.palette;
  var step = (maxVal - minVal) / (palette.length - 1);
  if (step <= 0) { step = 0.1; }
  
  // Descriptions for each group.
  var interpretations = [
    'Earliest greenup (early start)',
    'Slightly later greenup',
    'Intermediate greenup timing',
    'Later greenup (delayed start)',
    'Latest greenup (highest value)'
  ];
  
  // Build the combined legend rows.
  for (var i = 0; i < palette.length; i++) {
    var value = minVal + step * i;
    var row = ui.Panel({
      widgets: [
        ui.Label({
          style: {
            backgroundColor: palette[i],
            padding: '12px',
            margin: '2px 0',
            border: '1px solid #333'
          }
        }),
        ui.Label({
          value: value.toFixed(2) + ' : ' + interpretations[i],
          style: { fontSize: '14px', margin: '0 0 2px 8px' }
        })
      ],
      layout: ui.Panel.Layout.Flow('horizontal')
    });
    combinedLegend.add(row);
  }
  
  Map.add(combinedLegend);
  
  // === Section 7: Optional Export ===
  Export.image.toDrive({
    image: meanGreenup.clip(bajaPeninsulaGeometry),
    description: 'Baja_Peninsula_Mean_Greenup',
    folder: 'GEE_Exports',
    scale: 500,
    region: bajaPeninsulaGeometry,
    maxPixels: 1e13
  });
});
