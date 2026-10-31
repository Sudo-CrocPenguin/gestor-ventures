# gestor-ventures
gestión inteligente para emprendedores. Centraliza ventas, gastos, costos, agenda, clientes y apertura/cierre de caja en un solo lugar. Cuenta con un asistente basado en IA que analiza el contexto del negocio, responde preguntas financieras y genera recomendaciones y alertas para tomar mejores decisiones y hacer crecer el emprendimiento.

## Configurar el asistente de IA

El asistente usa Gemini y necesita una clave propia, que no se sube al repositorio. Sin ella la app muestra "Falta configurar la clave del asistente".

1. Saca una clave gratis en https://aistudio.google.com/apikey
2. Abre `local.properties` en la raiz del proyecto (esta ignorado por git) y agrega:
   `GEMINI_API_KEY=tu_clave`
3. Vuelve a compilar e instalar la app.
