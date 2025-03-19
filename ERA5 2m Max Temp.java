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

// Select the maximum_2m_air_temperature band
var temperature = dataset.select('maximum_2m_air_temperature');

// Compute the mean maximum temperature over the entire period
var meanMaxTemperature = temperature.mean().clip(bajaRegion); // Clip to Baja land area

// Define visualization parameters (adjusted for maximum temperatures)
var visualization = {
  bands: ['maximum_2m_air_temperature'],
  min: 290.0,  // ~17°C, lower limit for maximums in Baja
  max: 320.0,  // ~47°C, upper limit for maximums in Baja (hot desert summers)
  palette: ['#0000FF', '#1E90FF', '#00CED1', '#32CD32', '#FFFF00', '#FFA500', '#FF4500', '#FF0000'] // Detailed gradient
};

// Center the map on Baja California with higher zoom for detail
Map.setCenter(-112.0, 27.0, 7); // Zoom level 7 for more detail
Map.setOptions('SATELLITE'); // Satellite base layer

// Add the temperature layer to the map
Map.addLayer(meanMaxTemperature, visualization, 'Mean Maximum 2m Air Temperature (K) - 1979-2024');

// Add Baja outline with transparent color
Map.addLayer(ee.Image().paint(bajaRegion, 0, 1), {palette: ['#FFFFFF00']}, 'Baja Peninsula Outline');

// Add a legend at bottom-left
var legend = ui.Panel({
  style: {
    position: 'bottom-left',
    padding: '8px 15px',
    width: '300px'
  }
});

// Add title to legend
legend.add(ui.Label({
  value: 'Mean Maximum 2m Air Temperature in Baja Peninsula, 1979-2024',
  style: {fontWeight: 'bold', fontSize: '14px', margin: '0 0 8px 0'}
}));

// Add subtitle with units
legend.add(ui.Label({
  value: 'Temperature (K / °C / °F) - Severity and Description',
  style: {fontWeight: 'bold', fontSize: '12px', margin: '0 0 8px 0'}
}));

// Define temperature ranges, colors, descriptions, and severity (adjusted for maximums)
var palette = ['#0000FF', '#1E90FF', '#00CED1', '#32CD32', '#FFFF00', '#FFA500', '#FF4500', '#FF0000'];
var labels = [
  '< 290 K (~17°C / ~62.6°F)',
  '290–295 K (~17–22°C / ~62.6–71.6°F)',
  '295–300 K (~22–27°C / ~71.6–80.6°F)',
  '300–305 K (~27–32°C / ~80.6–89.6°F)',
  '305–310 K (~32–37°C / ~89.6–98.6°F)',
  '310–315 K (~37–42°C / ~98.6–107.6°F)',
  '315–320 K (~42–47°C / ~107.6–116.6°F)'
];
var descriptions = [
  'Mild maximums, northern Baja winters. Low impact.',
  'Warm maximums, central Baja summers. Moderate comfort.',
  'Hot maximums, southern Baja summers. Moderate heat stress.',
  'Very hot, southern deserts in peak summer. High heat stress.',
  'Extreme heat, rare but possible in deserts. Severe stress.',
  'Intense heat, critical for human safety. Extreme stress.',
  'Dangerous heat, life-threatening if sustained. Critical stress.'
];
var severity = [
  'Low',
  'Low to Moderate',
  'Moderate',
  'High',
  'Severe',
  'Extreme',
  'Critical'
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
        value: labels[i] + '\n' + descriptions[i] + ' Severity: ' + severity[i],
        style: {margin: '0 0 4px 4px', maxWidth: '250px', whiteSpace: 'normal'}
      })
    ],
    layout: ui.Panel.Layout.Flow('horizontal')
  }));
}

// Add the legend to the map
Map.add(legend);