// Load administrative boundaries (FAO GAUL dataset)
var admin1 = ee.FeatureCollection('FAO/GAUL/2015/level1');

// Filter to Baja California and Baja California Sur
var bajaStates = admin1.filter(
  ee.Filter.or(
    ee.Filter.eq('ADM1_NAME', 'Baja California'),
    ee.Filter.eq('ADM1_NAME', 'Baja California Sur')
  )
);

// Combine the two states into a single geometry and ensure it's land-only
var bajaRegion = bajaStates.geometry().dissolve();

// Load the ERA5 MONTHLY dataset for the full range
var dataset = ee.ImageCollection('ECMWF/ERA5/MONTHLY')
  .filter(ee.Filter.date('1979-01-01', '2024-12-31')) // Full range: 1979 to Dec 2024
  .filterBounds(bajaRegion); // Filter to Baja

// Select the mean_2m_air_temperature band
var temperature = dataset.select('mean_2m_air_temperature');

// Compute the mean temperature over the entire period
var meanTemperature = temperature.mean().clip(bajaRegion); // Clip to Baja land area

// Define visualization parameters
var visualization = {
  bands: ['mean_2m_air_temperature'],
  min: 280.0,  // ~7°C
  max: 300.0,  // ~27°C
  palette: ['#0000FF', '#1E90FF', '#00CED1', '#32CD32', '#FFFF00', '#FFA500', '#FF4500', '#FF0000'] // Detailed gradient
};

// Center the map on Baja California with higher zoom for detail
Map.setCenter(-112.0, 27.0, 7); // Zoom level 7 for more detail
Map.setOptions('SATELLITE'); // Satellite base layer

// Add the temperature layer to the map
Map.addLayer(meanTemperature, visualization, 'Mean 2m Air Temperature (K) - 1979-2024');

// Add Baja outline for reference
Map.addLayer(ee.Image().paint(bajaRegion, 0, 1), {palette: ['white']}, 'Baja Peninsula Outline');

// Add a legend
var legend = ui.Panel({
  style: {
    position: 'bottom-left',
    padding: '8px 15px'
  }
});

// Add title to legend
legend.add(ui.Label({
  value: 'Mean 2m Air Temperature',
  style: {fontWeight: 'bold', fontSize: '14px'}
}));

// Define temperature ranges and colors
var palette = ['#0000FF', '#1E90FF', '#00CED1', '#32CD32', '#FFFF00', '#FFA500', '#FF4500', '#FF0000'];
var range = [280, 282.5, 285, 287.5, 290, 292.5, 295, 300];
var labels = [
  '< (~9°C / ~48.8°F)',
  '(~9–12°C / ~48.8–53.3°F)',
  '(~12–14.5°C / ~53.3–57.8°F)',
  '(~14.5–17°C / ~57.8–62.3°F)',
  '(~17–19.5°C / ~62.3–66.8°F)',
  '(~19.5–22°C / ~66.8–71.3°F)',
  '(~22–27°C / ~71.3–80.3°F)'
];

// Create legend entries
for (var i = 0; i < palette.length - 1; i++) {
  legend.add(ui.Panel({
    widgets: [
      ui.Label({
        style: {
          backgroundColor: palette[i],
          padding: '5px',
          margin: '0 0 4px 0'
        }
      }),
      ui.Label({
        value: labels[i],
        style: {margin: '0 0 4px 4px'}
      })
    ],
    layout: ui.Panel.Layout.Flow('horizontal')
  }));
}

// Add the legend to the map
Map.add(legend);

// Add a simple label for context
var mapLabel = ui.Panel({style: {position: 'bottom-left', padding: '8px 15px'}});
mapLabel.add(ui.Label('Mean 2m Air Temperature (K) in Baja Peninsula, 1979-2024'));
Map.add(mapLabel);