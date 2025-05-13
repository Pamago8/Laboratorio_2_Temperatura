import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.ValueMarker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import datechooser.beans.DateChooserCombo;
import entidades.Temperatura;
import servicios.ServicioTemperaturaCiudad;

public class FrmTemperaturaCiudad extends JFrame {

    private JComboBox cmbCiudad;
    private DateChooserCombo dccDesde, dccHasta;
    private JTabbedPane tpTemperaturaCiudad;
    private JPanel pnlGrafica;
    private JPanel pnlEstadisticas;

    private List<String> ciudades;
    private List<Temperatura> datos;

    public FrmTemperaturaCiudad() {

        setTitle("Temperatura Ciudad");
        setSize(700, 400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JToolBar tb = new JToolBar();

        JButton btnGraficar = new JButton();
        btnGraficar.setIcon(new ImageIcon(getClass().getResource("/iconos/temperatura.png")));
        btnGraficar.setToolTipText("Grafica Temperaturas vs Fecha");
        btnGraficar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnGraficarClick();
            }
        });
        tb.add(btnGraficar);

        JButton btnCalcularEstadisticas = new JButton();
        btnCalcularEstadisticas.setIcon(new ImageIcon(getClass().getResource("/iconos/temperatura-alta.png")));
        btnCalcularEstadisticas.setToolTipText("Estadísticas de la ciudad seleccionada");
        btnCalcularEstadisticas.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCalcularEstadisticasClick();
            }
        });
        tb.add(btnCalcularEstadisticas);

        // Contenedor con BoxLayout (vertical)
        JPanel pnlTemperaturas = new JPanel();
        pnlTemperaturas.setLayout(new BoxLayout(pnlTemperaturas, BoxLayout.Y_AXIS));

        JPanel pnlDatosProceso = new JPanel();
        pnlDatosProceso.setPreferredSize(new Dimension(pnlDatosProceso.getWidth(), 50)); // Altura fija de 100px
        pnlDatosProceso.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        pnlDatosProceso.setLayout(null);

        JLabel lblTemperatura = new JLabel("Ciudad");
        lblTemperatura.setBounds(10, 10, 100, 25);
        pnlDatosProceso.add(lblTemperatura);

        cmbCiudad = new JComboBox();
        // cmbCiudad.setBounds(110, 10, 100, 25);
        // pnlDatosProceso.add(cmbCiudad);

        dccDesde = new DateChooserCombo();
        dccDesde.setBounds(110, 10, 100, 25);
        pnlDatosProceso.add(dccDesde);

        dccHasta = new DateChooserCombo();
        dccHasta.setBounds(220, 10, 100, 25);
        pnlDatosProceso.add(dccHasta);

        pnlGrafica = new JPanel();
        JScrollPane spGrafica = new JScrollPane(pnlGrafica);

        pnlEstadisticas = new JPanel();

        tpTemperaturaCiudad = new JTabbedPane();
        tpTemperaturaCiudad.addTab("Gráfica", spGrafica);
        tpTemperaturaCiudad.addTab("Estadísticas", pnlEstadisticas);

        // Agregar componentes
        pnlTemperaturas.add(pnlDatosProceso);
        pnlTemperaturas.add(tpTemperaturaCiudad);

        getContentPane().add(tb, BorderLayout.NORTH);
        getContentPane().add(pnlTemperaturas, BorderLayout.CENTER);

        cargarDatos();
    }

    private void cargarDatos() {
        datos = ServicioTemperaturaCiudad.getDatos(System.getProperty("user.dir") + "/src/datos/Temperaturas.csv");
        ciudades = ServicioTemperaturaCiudad.getCiudades(datos);
        DefaultComboBoxModel dcm = new DefaultComboBoxModel(ciudades.toArray());
        cmbCiudad.setModel(dcm);

    }

    private void btnGraficarClick() {
    LocalDate desde = dccDesde.getSelectedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate hasta = dccHasta.getSelectedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    // Filtrar solo por fechas
    List<Temperatura> datosFiltrados = datos.stream()
        .filter(d -> {
            LocalDate fecha = d.getFecha();
            return (fecha.isEqual(desde) || fecha.isAfter(desde)) &&
                   (fecha.isEqual(hasta) || fecha.isBefore(hasta));
        })
        .collect(Collectors.toList());

    // Agrupar por ciudad y calcular promedio de temperatura
    Map<String, Double> promedios = datosFiltrados.stream()
        .collect(Collectors.groupingBy(
            Temperatura::getCiudad,
            Collectors.averagingDouble(Temperatura::getTemperatura)
        ));

    // Crear dataset con los promedios
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    promedios.forEach((ciudad, promedio) -> {
        dataset.addValue(promedio, "Promedio", ciudad);
    });

    // Crear la gráfica
    JFreeChart chart = ChartFactory.createBarChart(
        "Promedio de Temperatura por Ciudad",
        "Ciudad",
        "°C",
        dataset
    );

    // Configurar el gráfico
    CategoryPlot plot = chart.getCategoryPlot();
    plot.getRangeAxis().setRange(0, 30);

    // Mostrar en el panel
    pnlGrafica.removeAll();
    pnlGrafica.setLayout(new BorderLayout());
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new Dimension(300, 200));
    pnlGrafica.add(chartPanel, BorderLayout.CENTER);
    pnlGrafica.validate();
    pnlGrafica.repaint();

    tpTemperaturaCiudad.setSelectedIndex(0);
}


    private void btnCalcularEstadisticasClick() {
    if (cmbCiudad.getSelectedIndex() >= 0) {
        String ciudad = (String) cmbCiudad.getSelectedItem();
        LocalDate desde = dccDesde.getSelectedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate hasta = dccHasta.getSelectedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Filtrar datos por el rango de fechas
        List<Temperatura> datosFiltrados = datos.stream()
            .filter(d -> {
                LocalDate fecha = d.getFecha();
                return (fecha.isEqual(desde) || fecha.isAfter(desde)) &&
                       (fecha.isEqual(hasta) || fecha.isBefore(hasta));
            })
            .collect(Collectors.toList());

        // Calcular promedios por ciudad
        Map<String, Double> promedios = datosFiltrados.stream()
            .collect(Collectors.groupingBy(
                Temperatura::getCiudad,
                Collectors.averagingDouble(Temperatura::getTemperatura)
            ));

        // Encontrar ciudad más y menos calurosa
        String ciudadMasCalurosa = "";
        String ciudadMenosCalurosa = "";
        double maxTemp = Double.MIN_VALUE;
        double minTemp = Double.MAX_VALUE;
        
        for (Map.Entry<String, Double> entry : promedios.entrySet()) {
            if (entry.getValue() > maxTemp) {
                maxTemp = entry.getValue();
                ciudadMasCalurosa = entry.getKey();
            }
            if (entry.getValue() < minTemp) {
                minTemp = entry.getValue();
                ciudadMenosCalurosa = entry.getKey();
            }
        }

        // Mostrar estadísticas
        tpTemperaturaCiudad.setSelectedIndex(1);

        var estadisticas = ServicioTemperaturaCiudad.getEstadisticas(ciudad, desde, hasta, datos);
        pnlEstadisticas.removeAll();
        pnlEstadisticas.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Agregar márgenes

        int fila = 0;
        
        // Agregar estadísticas de la ciudad seleccionada
        for (var estadistica : estadisticas.entrySet()) {
            gbc.gridx = 0;
            gbc.gridy = fila;
            pnlEstadisticas.add(new JLabel(estadistica.getKey()), gbc);
            gbc.gridx = 1;
            pnlEstadisticas.add(new JLabel(String.format("%.2f", estadistica.getValue())), gbc);
            fila++;
        }

        // Agregar información de ciudades extremas
        gbc.gridx = 0;
        gbc.gridy = fila++;
        pnlEstadisticas.add(new JLabel("Ciudad más calurosa:"), gbc);
        gbc.gridx = 1;
        pnlEstadisticas.add(new JLabel(ciudadMasCalurosa + " (" + String.format("%.2f", maxTemp) + "°C)"), gbc);

        gbc.gridx = 0;
        gbc.gridy = fila++;
        pnlEstadisticas.add(new JLabel("Ciudad menos calurosa:"), gbc);
        gbc.gridx = 1;
        pnlEstadisticas.add(new JLabel(ciudadMenosCalurosa + " (" + String.format("%.2f", minTemp) + "°C)"), gbc);

        pnlEstadisticas.validate();
        pnlEstadisticas.repaint();
    }
}
}