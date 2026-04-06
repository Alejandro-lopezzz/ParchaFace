package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.AssistantActionDto;
import com.alejo.parchaface.dto.AssistantOptionDto;
import com.alejo.parchaface.dto.AssistantRequest;
import com.alejo.parchaface.dto.AssistantResponse;
import com.alejo.parchaface.dto.AssistantChatMessageDto;
import com.alejo.parchaface.dto.ClimaPronosticoDia;
import com.alejo.parchaface.dto.ClimaResponse;
import com.alejo.parchaface.dto.RadarPlace;
import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.service.AiAssistantService;
import com.alejo.parchaface.service.ClimaService;
import com.alejo.parchaface.service.EventoService;
import com.alejo.parchaface.service.PlacesService;
import com.alejo.parchaface.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

  private static final Pattern OPEN_EVENT_PATTERN =
    Pattern.compile("abrir evento\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

  private static final Pattern TWELVE_HOUR_PATTERN =
    Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern TWENTY_FOUR_HOUR_PATTERN =
    Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");

  private static final Pattern BUDGET_PATTERN =
    Pattern.compile("(?i)(?:\\$\\s*)?(\\d{1,3}(?:[\\.,\\s]\\d{3})+|\\d{4,9}|\\d+\\s*[kK])");

  private static final DateTimeFormatter HOUR_FORMAT =
    DateTimeFormatter.ofPattern("HH:mm");

  private static final String ROUTE_HOME = "/";
  private static final String ROUTE_CREATE_EVENT = "/create-event";
  private static final String ROUTE_EXPLORE = "/explore";
  private static final String ROUTE_COMMUNITY = "/community";
  private static final String ROUTE_PROFILE = "/profile";
  private static final String ROUTE_PREFERENCES = "/preferencias";
  private static final String ROUTE_MAP = "/mapa";
  private static final String ROUTE_LOGIN = "/login";

  private final EventoService eventoService;
  private final UsuarioService usuarioService;
  private final PlacesService placesService;
  private final ClimaService climaService;

  private final Map<String, ConversationMemory> memoryStore = new ConcurrentHashMap<>();

  public AiAssistantServiceImpl(
    EventoService eventoService,
    UsuarioService usuarioService,
    PlacesService placesService,
    ClimaService climaService
  ) {
    this.eventoService = eventoService;
    this.usuarioService = usuarioService;
    this.placesService = placesService;
    this.climaService = climaService;
  }

  @Override
  public AssistantResponse chat(AssistantRequest request, Principal principal) {
    String originalMessage = request != null && request.message() != null
      ? request.message().trim()
      : "";

    String normalizedMessage = normalize(originalMessage);
    String currentRoute = request != null ? safe(request.currentRoute()) : "";

    if (normalizedMessage.isBlank()) {
      return simpleReply("Escríbeme algo y te ayudo con eventos, planes, recomendaciones, rutas dentro de ParchaFace, pagos, privacidad o términos.");
    }

    String conversationKey = buildConversationKey(principal, request);
    ConversationMemory memory = memoryStore.computeIfAbsent(conversationKey, key -> new ConversationMemory());
    memory.touch();

    mergePageContext(request, memory);
    applyAwaitingAnswer(originalMessage, memory);
    mergeDetectedContext(originalMessage, memory);
    mergeHistoryContext(request, memory);

    if (containsProfanity(normalizedMessage)) {
      return simpleReply("Ese mensaje contiene lenguaje inapropiado. Por favor escríbelo de otra forma para poder ayudarte.");
    }

    if (isDirectManipulationRequest(normalizedMessage)) {
      return simpleReply("""
                    No puedo crear ni editar eventos por ti directamente.
                    Tú eres quien conoce la información correcta del evento y yo no puedo hacerme responsable si hay errores en datos como fecha, hora, dirección, precio o descripción.
                    Lo que sí puedo hacer es explicarte el paso a paso para que lo hagas tú fácilmente.
                    """.trim());
    }

    AssistantResponse currentEventQuestion = handleCurrentEventQuestion(normalizedMessage, memory);
    if (currentEventQuestion != null) return currentEventQuestion;

    AssistantResponse resultListFollowUp = handleResultListFollowUp(normalizedMessage, memory);
    if (resultListFollowUp != null) return resultListFollowUp;

    if (memory.mode == GuidedMode.PLAN) {
      return handlePlanFlow(principal, memory);
    }

    if (memory.mode == GuidedMode.SEARCH) {
      return handleGuidedSearch(principal, memory, memory.usePreferenceCategories);
    }

    AssistantResponse openEventResponse = handleOpenEventById(normalizedMessage, memory);
    if (openEventResponse != null) return openEventResponse;

    AssistantResponse randomNearbyEventResponse = handleRandomNearbyEventRequest(normalizedMessage, memory);
    if (randomNearbyEventResponse != null) return randomNearbyEventResponse;

    AssistantResponse openEventByCityResponse = handleOpenEventByCity(originalMessage, normalizedMessage, memory);
    if (openEventByCityResponse != null) return openEventByCityResponse;

    AssistantResponse openEventByTitleResponse = handleOpenEventByTitle(originalMessage, normalizedMessage, memory);
    if (openEventByTitleResponse != null) return openEventByTitleResponse;

    AssistantResponse topicBasedResponse = handleSpecificTopicEventQuery(principal, originalMessage, normalizedMessage, memory);
    if (topicBasedResponse != null) return topicBasedResponse;

    AssistantResponse discoveryFollowUpResponse = handleDiscoveryFollowUp(principal, originalMessage, normalizedMessage, memory);
    if (discoveryFollowUpResponse != null) return discoveryFollowUpResponse;

    AssistantResponse multiCityAvailability = handleMultiCityAvailabilityQuestion(principal, originalMessage, normalizedMessage, memory);
    if (multiCityAvailability != null) {
      return multiCityAvailability;
    }

    AssistantResponse smallTalk = handleSmallTalk(normalizedMessage);
    if (smallTalk != null) {
      return smallTalk;
    }

    AssistantResponse guide = buildHowToResponse(normalizedMessage, currentRoute);
    if (guide != null) {
      return guide;
    }

    if (isGoToFeatureIntent(normalizedMessage)) {
      AssistantResponse goTo = buildGoToFeatureResponse(normalizedMessage);
      if (goTo != null) {
        return goTo;
      }
    }

    if (isAboutParchaFaceQuestion(normalizedMessage)) {
      return simpleReply("""
                ParchaFace es una plataforma digital para descubrir, crear y gestionar eventos sociales, deportivos, culturales y académicos.
                Funciona como intermediaria entre organizadores y participantes, permitiendo explorar planes, publicar eventos, interactuar con la comunidad y organizar mejor la experiencia dentro de la app.
                """.trim());
    }

    if (isPaymentMethodsQuestion(normalizedMessage)) {
      return simpleReply("En ParchaFace puedes pagar con tarjeta de crédito, tarjeta débito, PSE y Nequi.");
    }

    if (isTermsQuestion(normalizedMessage)) {
      return simpleReply("""
                Los Términos y Condiciones regulan el uso de ParchaFace.
                Allí se establece que la plataforma permite crear, publicar y explorar eventos; que el usuario debe registrarse con información verídica; que no puede publicar contenido ilegal, ofensivo o fraudulento; y que los organizadores son responsables de la veracidad y legalidad de los eventos que publican.
                También se aclara que ParchaFace puede actualizar estos términos en cualquier momento y que se rigen por la legislación de la República de Colombia.
                """.trim());
    }

    if (isPrivacyQuestion(normalizedMessage)) {
      return simpleReply("""
                La Política de Privacidad explica qué datos recopila ParchaFace, para qué se usan y cómo se protegen.
                Se recopilan datos de identificación, información relacionada con eventos, datos técnicos como IP, navegador o dispositivo, y datos que el usuario agregue voluntariamente en su perfil.
                Esos datos se usan para autenticación, gestión de eventos, mejora del servicio, seguridad y comunicaciones relacionadas con la plataforma.
                También se indica que los datos no se venden ni se comparten con terceros sin autorización del usuario, salvo obligación legal o cuando sea necesario para prestar el servicio.
                """.trim());
    }

    if (isConductRulesQuestion(normalizedMessage)) {
      return simpleReply(buildConductRulesReply());
    }

    if (isAvailableEventsQuestion(normalizedMessage)) {
      return buildAvailableEventsResponse(principal, memory);
    }

    if (isTransportQuestion(normalizedMessage)) {
      return simpleReply(buildTransportDirectoryReply(memory.city));
    }

    if (isEmergencyQuestion(normalizedMessage)) {
      return simpleReply(buildEmergencyReply(memory.city));
    }

    if (isRouteQuestion(normalizedMessage)) {
      if (containsAny(normalizedMessage, "comentarios", "comentario", "comments", "comentar") && currentRoute.startsWith("/event/")) {
        return new AssistantResponse(
          "Te explico y además te llevo: en el detalle del evento baja un poco y encontrarás la sección de comentarios. Te muevo hasta allí.",
          List.of(new AssistantActionDto("scroll", null, "comments-section", null, null)),
          List.of()
        );
      }

      if (containsAny(normalizedMessage, "community", "comunidad", "discusion", "discusión", "discutir")) {
        return new AssistantResponse(
          "La sección community es el espacio donde puedes ver publicaciones, discusiones e interactuar con la comunidad. Te llevo ahora.",
          List.of(new AssistantActionDto("navigate", ROUTE_COMMUNITY, null, null, null)),
          List.of()
        );
      }

      if (containsAny(normalizedMessage, "perfil", "mi perfil", "profile")) {
        return new AssistantResponse(
          "En tu perfil puedes ver tu actividad, tus eventos creados y tus eventos inscritos. Te llevo ahora.",
          List.of(new AssistantActionDto("navigate", ROUTE_PROFILE, null, null, null)),
          List.of()
        );
      }

      if (containsAny(normalizedMessage, "mapa", "map")) {
        return new AssistantResponse(
          "Te llevo al mapa para que veas los eventos por ubicación.",
          List.of(new AssistantActionDto("navigate", ROUTE_MAP, null, null, null)),
          List.of()
        );
      }

      if (containsAny(normalizedMessage, "inicio", "pagina principal", "página principal", "home")) {
        return new AssistantResponse(
          "Te llevo a la página principal.",
          List.of(new AssistantActionDto("navigate", ROUTE_HOME, null, null, null)),
          List.of()
        );
      }
    }

    AssistantResponse moodAwareResponse = handleMoodRecommendationIntent(principal, memory, originalMessage, normalizedMessage);
    if (moodAwareResponse != null) return moodAwareResponse;

    AssistantResponse weatherAwareResponse = handleWeatherRecommendationIntent(principal, memory, originalMessage, normalizedMessage);
    if (weatherAwareResponse != null) return weatherAwareResponse;

    if (isPlanIntent(normalizedMessage)) {
      memory.resetGuided();
      mergePageContext(request, memory);
      mergeDetectedContext(originalMessage, memory);
      memory.mode = GuidedMode.PLAN;
      memory.intentQuery = originalMessage;
      return handlePlanFlow(principal, memory);
    }

    if (isPreferencesRecommendationIntent(normalizedMessage)) {
      memory.resetGuided();
      mergePageContext(request, memory);
      mergeDetectedContext(originalMessage, memory);
      memory.mode = GuidedMode.SEARCH;
      memory.usePreferenceCategories = true;
      memory.intentQuery = originalMessage;
      return handleGuidedSearch(principal, memory, true);
    }

    if (isEventSearchIntent(normalizedMessage)) {
      memory.resetGuided();
      mergePageContext(request, memory);
      mergeDetectedContext(originalMessage, memory);
      memory.mode = GuidedMode.SEARCH;
      memory.usePreferenceCategories = false;
      memory.intentQuery = originalMessage;
      return handleGuidedSearch(principal, memory, false);
    }

    if (isGoToFeatureIntent(normalizedMessage) && containsAny(normalizedMessage, "inicio", "pagina principal", "página principal", "home")) {
      return replyWithNavigate("Te llevo a la página principal.", ROUTE_HOME);
    }

    if (isGoToFeatureIntent(normalizedMessage) && containsAny(normalizedMessage, "community", "comunidad")) {
      return replyWithNavigate("Te llevo a la sección community.", ROUTE_COMMUNITY);
    }

    if (isGoToFeatureIntent(normalizedMessage) && containsAny(normalizedMessage, "perfil", "mi perfil")) {
      return replyWithNavigate("Te llevo a tu perfil.", ROUTE_PROFILE);
    }

    if (isGoToFeatureIntent(normalizedMessage) && containsAny(normalizedMessage, "preferencias", "mis gustos", "mis categorias", "mis categorías")) {
      return replyWithNavigate("Te llevo a preferencias para que ajustes tus gustos.", ROUTE_PREFERENCES);
    }

    if (isGoToFeatureIntent(normalizedMessage) && containsAny(normalizedMessage, "mapa")) {
      return replyWithNavigate("Te llevo al mapa de eventos.", ROUTE_MAP);
    }

    if (isGoToFeatureIntent(normalizedMessage) && isExploreNavigationIntent(normalizedMessage)) {
      return replyWithNavigate("Te llevo a explorar eventos.", ROUTE_EXPLORE);
    }

    if (!seemsRelevantToParchaFace(normalizedMessage)) {
      return simpleReply("""
                En eso no puedo ayudarte mucho. Soy el asistente de ParchaFace y estoy enfocado en la plataforma.
                Puedo ayudarte con eventos, planes, rutas dentro de la app y procedimientos del sistema.
                """.trim());
    }

    return simpleReply("""
            No te entendí del todo. Si quieres, puedes decírmelo de otra forma.
            Por ejemplo: "cómo creo un evento", "llévame al inicio", "recomiéndame un evento de música gratis" o "qué eventos hay disponibles".
            """.trim());
  }

  private AssistantResponse handleSmallTalk(String message) {
    String trimmed = safe(message);
    String normalized = normalize(message);

    if (trimmed.matches("^(hola+|holi+s*|holis|holiss|buenas+|hello+|hi+|hey+|ey+|oe+|epa+|aja+|qlq|klk|que mas|que mas pues|que hubo|quiubo|kiubo|khubo|q hubo|q'hubo|que se dice|como fue|hablalo|hablame|alo+|alooo|buenass|wenas|wena+s*)$")) {
      return simpleReply("Hola 👋 ¿Qué quieres hacer en ParchaFace? Puedo recomendarte eventos, armarte un plan o llevarte al mapa de eventos.");
    }

    if (trimmed.matches("^(buenos dias|buen dia|buenas tardes|buenas noches|feliz dia|feliz tarde|feliz noche|wenas|wena+s*)$")) {
      return simpleReply("Hola 👋 Bienvenido a ParchaFace. Dime qué te gustaría hacer y te ayudo.");
    }

    if (containsAny(normalized,
      "oe y entonces", "oe entonces", "y entonces", "entonces que", "entonces qué",
      "aja y entonces", "que mas pues", "que hubo pues", "khubo pues", "que se dice",
      "y bueno", "bueno y entonces", "bueno pues", "ajá y qué", "ajá entonces")) {
      return simpleReply("Aquí estoy 😄 Dime qué quieres hacer y te ayudo. Por ejemplo: buscar eventos, armarte un plan o llevarte al mapa.");
    }

    if (trimmed.matches("^(mucho gusto|un gusto|encantado|encantada|encantao|encantaa|gustazo|un placer)$")) {
      return simpleReply("Mucho gusto 😄 Yo soy tu asistente de ParchaFace. Cuando quieras te ayudo con eventos, planes o rutas dentro de la app.");
    }

    if (trimmed.matches("^(gracias+|muchas gracias|mil gracias|graciaas|graciass|thanks|thank you|todo bien|todo bn|te lo agradezco|muy amable|bacano gracias)$")) {
      return simpleReply("Con gusto 😄 Si quieres, sigo ayudándote con eventos, planes o rutas dentro de la app.");
    }

    if (trimmed.matches("^(chao|adios|adiós|nos vemos|hasta luego|hasta pronto|bye|byee+|hablamos|la buena|me abro|me fui|nos pillamos|nos pillamo)$")) {
      return simpleReply("Chao 👋 Que te vaya súper. Cuando quieras, vuelves y te ayudo con otro plan o evento.");
    }

    if (trimmed.matches("^(como estas|cómo estás|que tal|qué tal|todo bien|todo bn|todo bien o que|todo bien o qué|melo o no|como vas|cómo vas|que cuentas|qué cuentas|todo sano)$")) {
      return simpleReply("Todo bien 😄 Listo para ayudarte con ParchaFace. ¿Qué necesitas?");
    }

    if (trimmed.matches("^(jaja+|jajaja+|jeje+|jiji+|xd+|lol+|jsjs+|ajaj+|xdxd+)$")) {
      return simpleReply("Jajaja 😄 no sé de qué te ríes, pero acá sí te puedo encontrar eventos y planes que te hagan pasar bueno.");
    }

    if (trimmed.matches("^(ok|okay|okey|oki|okis|vale|listo|de una|dale|d1|perfecto|entiendo|hagale|hágale|eso es todo|ya lo dijo|suena bien|me sirve|copiado)$")) {
      return simpleReply("De una 🙌 dime qué necesitas y seguimos.");
    }

    if (trimmed.matches("^(perdon|perdón|disculpa|disculpame|discúlpame|lo siento|sorry|srry)$")) {
      return simpleReply("Tranqui 😄 seguimos. Dime qué necesitas en ParchaFace y te ayudo.");
    }

    return null;
  }

  private AssistantResponse buildHowToResponse(String message, String currentRoute) {
    if (!containsHowToCue(message)) {
      return null;
    }

    if ((containsAny(message, "evento") && containsAny(message, "crear", "publicar", "hacer", "subir", "montar"))
      || containsAny(message,
      "crear evento", "crear un evento", "publicar evento", "publicar un evento",
      "hacer evento", "hacer un evento", "creo un evento", "como creo un evento",
      "cómo creo un evento", "como hago un evento", "cómo hago un evento",
      "como publico un evento", "cómo publico un evento", "como subo un evento",
      "cómo subo un evento", "como monto un evento", "cómo monto un evento",
      "como armo un evento", "cómo armo un evento")) {
      return new AssistantResponse(
        """
                    Claro. Paso a paso para crear un evento en ParchaFace:

                    1. Entra al formulario de crear evento.
                    2. Escribe el título del evento.
                    3. Selecciona la categoría.
                    4. Define si es gratis o de pago.
                    5. Elige fecha y hora.
                    6. Agrega ciudad, ubicación y dirección.
                    7. Revisa la ubicación en el mapa si está disponible.
                    8. Escribe la descripción y los detalles importantes.
                    9. Guarda y envía la solicitud.
                    10. Esa solicitud la revisa el administrador y, si todo está bien, el evento se aprueba y se publica.

                    Si quieres, te llevo ahora mismo al formulario para crear evento.
                    """.trim(),
        List.of(),
        List.of(
          new AssistantOptionDto("go-create-event", "Llévame a crear evento", "llévame a crear evento")
        )
      );
    }

    if (containsAny(message, "editar perfil", "editar mi perfil", "como edito mi perfil", "cómo edito mi perfil",
      "cambiar perfil", "actualizar perfil", "modificar perfil", "cambiar mi foto", "cambiar mis datos",
      "arreglar mi perfil", "ajustar mi perfil", "editar mis datos", "cambiar la foto de perfil")) {
      return simpleReply("""
                Paso a paso para editar tu perfil en ParchaFace:

                1. Ve a tu perfil.
                2. Busca la opción de editar perfil o la parte de datos personales.
                3. Cambia la información que necesites, como nombre, foto o datos visibles.
                4. Guarda los cambios.
                5. Revisa que la información haya quedado actualizada correctamente.
                """.trim());
    }

    if (containsAny(message, "inicio", "pagina principal", "página principal", "home", "pantalla principal", "pantalla de inicio")) {
      return simpleReply("""
                Paso a paso para usar la página principal de ParchaFace:

                1. Entra al inicio.
                2. Revisa los eventos destacados o disponibles.
                3. Usa los filtros si quieres buscar algo más específico.
                4. Entra al detalle del evento que te interese.
                5. Desde allí puedes ver información, ubicación, precio y acciones disponibles.
                """.trim());
    }

    if (containsAny(message, "explorar", "buscar eventos", "ver eventos", "eventos disponibles", "explore", "mirar eventos", "vitrinear eventos")) {
      return simpleReply("""
                Paso a paso para explorar eventos en ParchaFace:

                1. Entra a la sección de explorar eventos.
                2. Revisa la lista de eventos disponibles.
                3. Usa filtros por categoría, ciudad, precio o lo que necesites.
                4. Abre el detalle del evento que te guste.
                5. Desde allí puedes decidir si quieres verlo mejor, comentarlo o inscribirte.
                """.trim());
    }

    if (containsAny(message, "mapa", "map", "ver mapa", "abrir mapa", "mapita")) {
      return simpleReply("""
                Paso a paso para usar el mapa en ParchaFace:

                1. Entra a la sección del mapa.
                2. Mira los eventos ubicados cerca de ti o en otra zona.
                3. Pulsa el marcador del evento que te interese.
                4. Revisa el resumen o entra al detalle completo.
                5. Desde allí puedes comparar ubicaciones y decidir cuál te conviene más.
                """.trim());
    }

    if (containsAny(message, "community", "comunidad", "discusiones", "discusion", "discusión", "foro", "publicaciones")) {
      return simpleReply("""
                Paso a paso para usar community en ParchaFace:

                1. Entra a la sección community.
                2. Revisa publicaciones o discusiones activas.
                3. Abre la discusión que te interese.
                4. Lee comentarios, responde o interactúa según lo que esté habilitado.
                5. Si quieres, también puedes crear una nueva publicación desde la sección correspondiente.
                """.trim());
    }

    if (containsAny(message, "crear publicacion", "crear publicación", "crear post", "publicar en community", "publicar en comunidad", "hacer una publicacion", "hacer una publicación", "subir un post")) {
      return simpleReply("""
                Paso a paso para crear una publicación en community:

                1. Entra a community.
                2. Ve a la opción de crear publicación.
                3. Escribe el contenido o tema que quieres compartir.
                4. Revisa que el texto esté claro.
                5. Publica el contenido.
                """.trim());
    }

    if (containsAny(message, "inscribirme", "inscribirse", "me inscribo", "me uno a un evento", "unirme a un evento", "entrar a un evento", "registrarme a un evento", "apuntarme a un evento", "meterme a un evento")) {
      return simpleReply("""
                Paso a paso para inscribirte a un evento:

                1. Busca el evento en explorar o en el mapa.
                2. Entra al detalle del evento.
                3. Revisa fecha, hora, precio y ubicación.
                4. Pulsa el botón de inscripción o compra si está disponible.
                5. Completa el proceso y revisa la confirmación.
                """.trim());
    }

    if (containsAny(message, "pagar", "comprar entrada", "comprar un evento", "pago de evento", "comprar boleta", "comprar boleto", "pagar una entrada", "hacer el pago", "como pago", "cómo pago")) {
      return simpleReply("""
                Paso a paso para pagar un evento en ParchaFace:

                1. Entra al detalle del evento.
                2. Revisa el valor y la información del evento.
                3. Pulsa la opción para comprar o pagar.
                4. Selecciona el método de pago disponible.
                5. Completa el proceso y verifica la confirmación final.
                """.trim());
    }

    if (containsAny(message, "comentario", "comentarios", "comentar", "dejar comentario", "poner comentario", "opinar", "escribir un comentario")) {
      if (currentRoute != null && currentRoute.startsWith("/event/")) {
        return new AssistantResponse(
          """
                        Paso a paso para comentar en un evento:

                        1. Abre el detalle del evento.
                        2. Baja hasta la sección de comentarios.
                        3. Escribe tu comentario.
                        4. Publica el mensaje.

                        Te llevo de una a la sección de comentarios.
                        """.trim(),
          List.of(new AssistantActionDto("scroll", null, "comments-section", null, null)),
          List.of()
        );
      }

      return simpleReply("""
                Paso a paso para comentar en un evento:

                1. Abre el detalle del evento.
                2. Baja hasta la sección de comentarios.
                3. Escribe tu comentario.
                4. Publícalo.
                """.trim());
    }

    if (containsAny(message, "preferencias", "mis gustos", "configurar preferencias", "editar preferencias", "ajustar gustos", "mis intereses")) {
      return simpleReply("""
                Paso a paso para configurar tus preferencias:

                1. Ve a la sección de preferencias.
                2. Selecciona las categorías que más te interesan.
                3. Guarda los cambios.
                4. Luego vuelve al asistente y pídele recomendaciones según tus gustos.
                """.trim());
    }

    if (containsAny(message, "login", "iniciar sesion", "iniciar sesión", "loguearme", "entrar a mi cuenta", "acceder")) {
      return simpleReply("""
                Paso a paso para iniciar sesión en ParchaFace:

                1. Entra a la pantalla de login.
                2. Escribe tu correo y tu contraseña.
                3. Pulsa el botón para iniciar sesión.
                4. Si los datos son correctos, entrarás a tu cuenta.
                """.trim());
    }

    if (containsAny(message, "registrarme", "registro", "crear cuenta", "abrir cuenta", "hacerme una cuenta")) {
      return simpleReply("""
                Paso a paso para registrarte en ParchaFace:

                1. Entra a la pantalla de registro.
                2. Completa tus datos personales.
                3. Crea tu contraseña.
                4. Revisa la información.
                5. Envía el formulario para crear tu cuenta.
                """.trim());
    }

    if (containsAny(message, "olvidé mi contraseña", "olvide mi contraseña", "recuperar contraseña", "cambiar contraseña", "renovar mi contraseña", "restablecer mi contraseña", "se me olvido la clave", "se me olvidó la clave", "olvide la clave", "olvidé la clave")) {
      return simpleReply("""
                Paso a paso para recuperar tu contraseña:

                1. Entra a la opción de forgot password.
                2. Escribe el correo asociado a tu cuenta.
                3. Revisa el código o paso de verificación.
                4. Confirma el código si se solicita.
                5. Crea la nueva contraseña y guarda el cambio.
                """.trim());
    }

    if (seemsRelevantToParchaFace(message)) {
      return simpleReply("Sí te puedo explicar ese procedimiento. Dime exactamente qué quieres hacer dentro de la página y te doy el paso a paso.");
    }

    return null;
  }

  private AssistantResponse buildGoToFeatureResponse(String message) {
    String detectedCity = detectCity(message);
    String detectedCategory = detectCategory(message);

    if (containsAny(message, "crear evento", "publicar evento")) {
      return new AssistantResponse(
        "Te llevo a crear evento.",
        List.of(new AssistantActionDto("navigate", "/create-event", null, null, null)),
        List.of()
      );
    }

    if (containsAny(message, "perfil", "profile")) {
      return new AssistantResponse(
        "Te llevo a tu perfil.",
        List.of(new AssistantActionDto("navigate", "/profile", null, null, null)),
        List.of()
      );
    }

    if (containsAny(message, "community", "comunidad", "discusion", "discusión")) {
      return new AssistantResponse(
        "Te llevo a community.",
        List.of(new AssistantActionDto("navigate", "/community", null, null, null)),
        List.of()
      );
    }

    if (containsAny(message, "preferencias", "gustos")) {
      return new AssistantResponse(
        "Te llevo a preferencias.",
        List.of(new AssistantActionDto("navigate", "/preferencias", null, null, null)),
        List.of()
      );
    }

    if (containsAny(message, "mapa", "ver mapa", "abrir mapa")) {
      Map<String, Object> query = new ConcurrentHashMap<>();
      if (!isBlank(detectedCity)) {
        query.put("q", detectedCity);
      }
      if (!isBlank(detectedCategory)) {
        query.put("category", detectedCategory);
      }

      return new AssistantResponse(
        !isBlank(detectedCity)
          ? "Te llevo al mapa con eventos en " + safe(detectedCity) + "."
          : "Te llevo al mapa de eventos.",
        List.of(new AssistantActionDto("navigate", "/mapa", null, null, query.isEmpty() ? null : query)),
        List.of()
      );
    }

    if (containsAny(message, "explorar", "explore", "ver eventos", "listado de eventos", "inicio de eventos")) {
      Map<String, Object> query = new ConcurrentHashMap<>();
      if (!isBlank(detectedCity)) {
        query.put("q", detectedCity);
      }
      if (!isBlank(detectedCategory)) {
        query.put("category", detectedCategory);
      }

      return new AssistantResponse(
        "Te llevo a explorar eventos.",
        List.of(new AssistantActionDto("navigate", "/explore", null, null, query.isEmpty() ? null : query)),
        List.of()
      );
    }

    return null;
  }

  private AssistantResponse handleGuidedSearch(Principal principal, ConversationMemory memory, boolean fromPreferences) {
    if (memory.budgetMax != null && memory.wantsFree == null) {
      memory.wantsFree = false;
    }

    if (fromPreferences) {
      Usuario usuario = getAuthenticatedUser(principal);
      if (usuario == null) {
        memory.resetGuided();
        return new AssistantResponse(
          "Para recomendarte según tus preferencias necesito que inicies sesión.",
          List.of(new AssistantActionDto("navigate", ROUTE_LOGIN, null, null, null)),
          List.of()
        );
      }

      List<String> categories = usuario.getCategoriasPreferidas() != null
        ? usuario.getCategoriasPreferidas()
        : List.of();

      if (categories.isEmpty()) {
        memory.resetGuided();
        return new AssistantResponse(
          "Todavía no tienes preferencias configuradas. Te llevo para que las completes.",
          List.of(new AssistantActionDto("navigate", ROUTE_PREFERENCES, null, null, null)),
          List.of()
        );
      }
    }

    if (memory.wantsFree == null) {
      memory.awaiting = Awaiting.FREE_OR_PAID;
      return askFreeOrPaid();
    }

    if (Boolean.FALSE.equals(memory.wantsFree) && memory.budgetMax == null) {
      memory.awaiting = Awaiting.BUDGET;
      return askBudget();
    }

    boolean canUseLocationOnly = fromPreferences
      && memory.referenceLat != null
      && memory.referenceLng != null;

    if (isBlank(memory.city) && !canUseLocationOnly) {
      memory.awaiting = Awaiting.CITY;
      return askCity();
    }

    if (memory.zone == null) {
      memory.zone = "__ANY__";
    }

    List<Evento> exactResults = searchStrictEvents(memory, principal);
    if (!exactResults.isEmpty()) {
      List<AssistantOptionDto> options = buildOptions(exactResults, memory, 5);

      String reply;
      if (fromPreferences && canUseLocationOnly) {
        reply = "Según tus gustos y cerca de ti, estas opciones te pueden interesar. Elige una y te llevo.";
      } else if (fromPreferences) {
        reply = "Según tus gustos y lo que me dijiste, estas opciones te pueden interesar. Elige una y te llevo.";
      } else {
        reply = "Encontré estas opciones según lo que me pediste. Elige una y te llevo.";
      }

      rememberDiscoveryContext(memory, fromPreferences ? DiscoveryContext.PREFERENCES : DiscoveryContext.SEARCH);
      memory.resetGuided();
      return new AssistantResponse(reply, List.of(), options);
    }

    List<Evento> similarResults = findAlternativeEvents(memory, principal);
    if (!similarResults.isEmpty()) {
      List<AssistantOptionDto> options = buildOptions(similarResults, memory, 5);

      rememberDiscoveryContext(memory, fromPreferences ? DiscoveryContext.PREFERENCES : DiscoveryContext.SEARCH);

      if (fromPreferences) {
        memory.resetGuided();
        return new AssistantResponse(
          "Según tus gustos, estas son las mejores opciones que encontré para ti. Elige una y te llevo.",
          List.of(),
          options
        );
      }

      String cityText = !isBlank(memory.city) ? " en " + memory.city : "";
      memory.resetGuided();
      return new AssistantResponse(
        "No encontré eventos disponibles con esos requisitos exactos" + cityText + ", pero sí encontré estas opciones parecidas.",
        List.of(),
        options
      );
    }

    String noResults = buildNoResultsMessage(memory);
    memory.resetGuided();
    return new AssistantResponse(
      noResults,
      List.of(new AssistantActionDto("navigate", ROUTE_EXPLORE, null, null, null)),
      List.of()
    );
  }

  private AssistantResponse handlePlanFlow(Principal principal, ConversationMemory memory) {
    if (memory.budgetMax != null && memory.wantsFree == null) {
      memory.wantsFree = false;
    }

    if (isBlank(memory.category)) {
      memory.awaiting = Awaiting.CATEGORY;
      return new AssistantResponse(
        "Antes de armarte el plan, dime qué tipo de evento quieres. Puede ser fiesta, música, deporte, gastronomía, networking, gaming o arte.",
        List.of(),
        List.of(
          new AssistantOptionDto("cat-fiestas", "Fiesta", "fiesta"),
          new AssistantOptionDto("cat-musica", "Música", "música"),
          new AssistantOptionDto("cat-deporte", "Deporte", "deporte"),
          new AssistantOptionDto("cat-gaming", "Gaming", "gaming")
        )

      );
    }

    if (memory.wantsFree == null) {
      memory.awaiting = Awaiting.FREE_OR_PAID;
      return askFreeOrPaid();
    }

    if (Boolean.FALSE.equals(memory.wantsFree) && memory.budgetMax == null) {
      memory.awaiting = Awaiting.BUDGET;
      return askBudget();
    }

    if (isBlank(memory.city) && (memory.referenceLat == null || memory.referenceLng == null)) {
      memory.awaiting = Awaiting.CITY;
      return askCity();
    }

    if (memory.zone == null) {
      memory.zone = "__ANY__";
    }

    if (memory.needsTransport == null) {
      memory.awaiting = Awaiting.TRANSPORT;
      return new AssistantResponse(
        "¿Quieres que incluya transporte en el plan?",
        List.of(),
        List.of(
          new AssistantOptionDto("transport-si", "Sí, inclúyelo", "sí, necesito transporte"),
          new AssistantOptionDto("transport-no", "No, sin transporte", "no, no necesito transporte")
        )
      );
    }

    if (memory.wantsFoodSuggestions == null) {
      memory.awaiting = Awaiting.FOOD;
      return new AssistantResponse(
        "¿Quieres que también te sugiera lugares para comer antes o después del evento?",
        List.of(),
        List.of(
          new AssistantOptionDto("food-si", "Sí, con comida cerca", "sí quiero lugares cercanos para comer"),
          new AssistantOptionDto("food-no", "No, sin comida", "no quiero comida")
        )
      );
    }

    List<Evento> exactResults = searchStrictEvents(memory, principal);
    if (!exactResults.isEmpty()) {
      Evento mainEvent = exactResults.get(0);
      List<RadarPlace> nearbyPlaces = getNearbyFood(mainEvent, memory);
      ClimaResponse weather = getWeather(mainEvent.getCiudad());

      String plan = buildPlanReply(mainEvent, nearbyPlaces, weather, memory);
      List<AssistantOptionDto> options = buildOptions(exactResults, memory, 3);

      rememberDiscoveryContext(memory, DiscoveryContext.PLAN);
      memory.resetGuided();
      return new AssistantResponse(plan, List.of(), options);
    }

    List<Evento> similarResults = findAlternativeEvents(memory, principal);
    if (!similarResults.isEmpty()) {
      Evento mainEvent = similarResults.get(0);
      List<RadarPlace> nearbyPlaces = getNearbyFood(mainEvent, memory);
      ClimaResponse weather = getWeather(mainEvent.getCiudad());

      String plan = buildPlanReply(mainEvent, nearbyPlaces, weather, memory);
      List<AssistantOptionDto> options = buildOptions(similarResults, memory, 3);

      rememberDiscoveryContext(memory, DiscoveryContext.PLAN);
      memory.resetGuided();
      return new AssistantResponse(plan, List.of(), options);
    }

    String noResults = buildNoResultsMessage(memory);
    memory.resetGuided();
    return new AssistantResponse(
      noResults,
      List.of(new AssistantActionDto("navigate", ROUTE_EXPLORE, null, null, null)),
      List.of()
    );
  }

  private AssistantResponse handleOpenEventById(String normalizedMessage, ConversationMemory memory) {
    Matcher matcher = OPEN_EVENT_PATTERN.matcher(normalizedMessage);
    if (!matcher.find()) {
      return null;
    }

    Integer eventId = Integer.valueOf(matcher.group(1));
    Evento evento = eventoService.getEventosPublicos()
      .stream()
      .filter(e -> Objects.equals(e.getIdEvento(), eventId))
      .findFirst()
      .orElse(null);

    memory.resetGuided();

    if (evento == null) {
      return simpleReply("No encontré ese evento entre los eventos públicos.");
    }

    return new AssistantResponse(
      "Te llevo a \"" + safe(evento.getTitulo()) + "\".",
      List.of(new AssistantActionDto("navigate", "/event/" + eventId, null, null, null)),
      List.of()
    );
  }

  private AssistantResponse handleOpenEventByCity(String originalMessage, String normalizedMessage, ConversationMemory memory) {
    if (!isGoToFeatureIntent(normalizedMessage)) {
      return null;
    }

    if (!containsAny(normalizedMessage, "evento", "eventos", "mapa", "planes")) {
      return null;
    }

    if (containsAny(normalizedMessage, "azar", "aleatorio", "random", "ramdom", "crear evento", "crear un evento")) {
      return null;
    }

    String city = detectCity(originalMessage);
    if (isBlank(city)) {
      return null;
    }

    List<Evento> cityEvents = eventoService.getEventosPublicos().stream()
      .filter(event -> matchesCity(event, city))
      .sorted(Comparator.comparingDouble(event -> distanceScoreForNavigation(event, memory)))
      .toList();

    memory.resetGuided();

    if (cityEvents.isEmpty()) {
      return simpleReply("No hay eventos disponibles en " + city + ".");
    }

    if (containsAny(normalizedMessage, "eventos", "mapa", "planes", "ver eventos")) {
      return new AssistantResponse(
        "Te llevo al mapa con los eventos de " + safe(city) + ".",
        List.of(new AssistantActionDto("navigate", ROUTE_MAP, null, null, Map.of("q", city))),
        List.of()
      );
    }

    Evento chosen = cityEvents.get(0);
    return new AssistantResponse(
      "Te llevo a un evento disponible en " + safe(city) + ": \"" + safe(chosen.getTitulo()) + "\".",
      List.of(new AssistantActionDto("navigate", "/event/" + chosen.getIdEvento(), null, null, null)),
      List.of()
    );
  }

  private AssistantResponse handleOpenEventByTitle(String originalMessage, String normalizedMessage, ConversationMemory memory) {
    if (!isGoToFeatureIntent(normalizedMessage)) {
      return null;
    }

    String requestedTitle = extractRequestedEventReference(originalMessage);
    if (isBlank(requestedTitle)) {
      return null;
    }

    Evento bestMatch = findBestEventByReference(requestedTitle, memory);
    if (bestMatch == null) {
      return null;
    }

    memory.resetGuided();
    return new AssistantResponse(
      "Te llevo a \"" + safe(bestMatch.getTitulo()) + "\".",
      List.of(new AssistantActionDto("navigate", "/event/" + bestMatch.getIdEvento(), null, null, null)),
      List.of()
    );
  }

  private String extractRequestedEventReference(String message) {
    if (isBlank(message)) {
      return null;
    }

    Matcher matcher = Pattern.compile(
      "(?i)(?:ll[eé]vame|abre|abrime|quiero ir|quiero entrar|vamos a|m[aá]ndame|mu[eé]strame|mostrame|ve a|ir a)\s+(?:al|a la|a el|a|el|la)?\s*(.+)$"
    ).matcher(message.trim());

    if (!matcher.find()) {
      return null;
    }

    String candidate = matcher.group(1)
      .replaceAll("(?i)^evento\s+", "")
      .replaceAll("(?i)^de\s+", "")
      .replaceAll("(?i)^otro\s+evento\s+de\s+", "")
      .replaceAll("(?i)^otro\s+de\s+", "")
      .replaceAll("(?i)^otro\s+", "")
      .trim();

    if (isBlank(candidate)) {
      return null;
    }

    String normalizedCandidate = normalize(candidate);

    if (containsAny(normalizedCandidate,
      "eventos", "crear evento", "crear un evento", "un evento al azar",
      "community", "perfil", "preferencias", "mapa", "explorar",
      "inicio", "pagina principal", "página principal")) {
      return null;
    }

    return candidate;
  }

  private AssistantResponse handleSpecificTopicEventQuery(Principal principal, String originalMessage, String normalizedMessage, ConversationMemory memory) {
    if (memory.mode == GuidedMode.PLAN || memory.awaiting != Awaiting.NONE) {
      return null;
    }

    if (!isBlank(detectCategory(originalMessage))) {
      return null;
    }

    String topicReference = extractSpecificTopicReference(originalMessage);
    if (isBlank(topicReference) || !isSpecificTopicEventIntent(normalizedMessage)) {
      return null;
    }

    memory.intentQuery = topicReference;
    memory.lastTopicQuery = topicReference;

    List<Evento> results = searchEventsByReference(topicReference, memory);

    rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);

    if (results.isEmpty()) {
      String cityText = !isBlank(memory.city) ? " en " + memory.city : "";
      memory.resetGuided();
      return simpleReply("No encontré eventos relacionados con \"" + safe(topicReference) + "\"" + cityText + " en este momento.");
    }

    List<AssistantOptionDto> options = buildOptions(results, memory, 5);

    Evento top = results.get(0);
    String cityText = !isBlank(top.getCiudad()) ? " en " + safe(top.getCiudad()) : "";
    memory.resetGuided();
    return new AssistantResponse(
      "Sí, encontré eventos relacionados con \"" + safe(topicReference) + "\". El más cercano a lo que buscas es \"" + safe(top.getTitulo()) + "\"" + cityText + ".",
      List.of(),
      options
    );
  }

  private AssistantResponse handleDiscoveryFollowUp(Principal principal, String originalMessage, String normalizedMessage, ConversationMemory memory) {
    if (memory.lastDiscoveryContext == DiscoveryContext.NONE) {
      return null;
    }

    boolean followUpCue = normalizedMessage.startsWith("y ")
      || normalizedMessage.startsWith("en ")
      || normalizedMessage.startsWith("de ")
      || normalizedMessage.startsWith("sobre ")
      || normalizedMessage.startsWith("entonces ")
      || containsAny(normalizedMessage, "y en", "y de", "y sobre", "entonces en", "entonces de");

    String followUpTopic = extractFollowUpTopicReference(originalMessage, normalizedMessage);

    boolean changedContext = !isBlank(detectCity(originalMessage))
      || !isBlank(detectCategory(originalMessage))
      || !isBlank(detectZone(originalMessage))
      || detectBudget(originalMessage) != null
      || !isBlank(followUpTopic);

    if (!followUpCue || !changedContext) {
      return null;
    }

    if (!isBlank(followUpTopic)) {
      memory.intentQuery = followUpTopic;
      memory.lastTopicQuery = followUpTopic;
    } else if (extractSearchKeywords(originalMessage).size() <= 2 && !isBlank(memory.lastTopicQuery)) {
      memory.intentQuery = memory.lastTopicQuery;
    } else if (!isBlank(originalMessage)) {
      memory.intentQuery = originalMessage;
      memory.lastTopicQuery = originalMessage;
    }

    if (!isBlank(memory.lastTopicQuery)) {
      List<Evento> results = searchEventsByReference(memory.lastTopicQuery, memory);

      if (!results.isEmpty()) {
        List<AssistantOptionDto> options = buildOptions(results, memory, 5);

        Evento top = results.get(0);
        rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
        memory.resetGuided();

        return new AssistantResponse(
          "Sí, para ese cambio encontré estas opciones. La que mejor encaja ahora es \"" + safe(top.getTitulo()) + "\""
            + (!isBlank(top.getCiudad()) ? " en " + safe(top.getCiudad()) : "") + ".",
          List.of(),
          options
        );
      }

      rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
      memory.resetGuided();

      return simpleReply(
        "No encontré eventos relacionados con \"" + safe(memory.lastTopicQuery) + "\""
          + (!isBlank(memory.city) ? " en " + safe(memory.city) : "")
          + " en este momento."
      );
    }

    if (memory.lastDiscoveryContext == DiscoveryContext.PLAN) {
      memory.mode = GuidedMode.PLAN;
      return handlePlanFlow(principal, memory);
    }

    if (memory.lastDiscoveryContext == DiscoveryContext.PREFERENCES) {
      memory.mode = GuidedMode.SEARCH;
      memory.usePreferenceCategories = true;
      return handleGuidedSearch(principal, memory, true);
    }

    if (memory.lastDiscoveryContext == DiscoveryContext.SEARCH) {
      memory.mode = GuidedMode.SEARCH;
      memory.usePreferenceCategories = false;
      return handleGuidedSearch(principal, memory, false);
    }

    rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
    return buildAvailableEventsResponse(principal, memory);
  }

  private AssistantResponse handleMultiCityAvailabilityQuestion(Principal principal, String originalMessage, String normalizedMessage, ConversationMemory memory) {
    if (!isAvailableEventsQuestion(normalizedMessage)) {
      return null;
    }

    List<String> cities = detectCities(originalMessage);
    if (cities.size() < 2) {
      return null;
    }

    List<Evento> all = eventoService.getEventosPublicos();
    List<Evento> results = all.stream()
      .filter(event -> cities.stream().anyMatch(city -> matchesCity(event, city)))
      .sorted(Comparator.comparingDouble(event -> distanceScoreForNavigation(event, memory)))
      .limit(6)
      .toList();

    if (results.isEmpty()) {
      return simpleReply("No encontré eventos disponibles en " + String.join(" o ", cities) + " en este momento.");
    }

    String joinedCities = String.join(" o ", cities);
    List<AssistantOptionDto> options = buildOptions(results, memory, 6);

    rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
    return new AssistantResponse(
      "Sí, encontré eventos en " + joinedCities + ". Te dejo varias opciones para que elijas una y te llevo.",
      List.of(),
      options
    );
  }

  private String extractSpecificTopicReference(String message) {
    if (isBlank(message)) {
      return null;
    }

    String trimmed = message.trim();

    List<Pattern> patterns = List.of(
      Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen|busca(?:me)?|consigue(?:me)?|encuentra(?:me)?|trae(?:me)?|mu[eé]strame|mostrame|ens[eé]ñame|recomi[eé]ndame|t[ií]rame|l[aá]nzame|ponme|dame|quiero|p[aá]same|su[eé]ltame)(?:\s+(?:alg[uú]n|alguna|alguito|algo|un|una))?\s*(?:evento|eventos|concierto|conciertos|fiesta|fiestas|show|shows|plan|planes|rumba|rumbas|actividad|actividades)?\s*(?:de|sobre|con)\s+(.+)$"),
      Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen)(?:\s+(?:alg[uú]n|alguna|algo|un|una))?\s*(?:evento|eventos|concierto|conciertos|fiesta|fiestas|show|shows|plan|planes|actividad|actividades)\s+(.+)$"),
      Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen|busca(?:me)?|consigue(?:me)?|encuentra(?:me)?|trae(?:me)?|mu[eé]strame|mostrame|ens[eé]ñame|recomi[eé]ndame|t[ií]rame|l[aá]nzame|ponme|dame|quiero|p[aá]same|su[eé]ltame)(?:\s+(?:alg[uú]n|alguna|algo|un|una))?\s+(.+)$")
    );

    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(trimmed);
      if (matcher.find()) {
        String cleaned = cleanTopicReference(matcher.group(1));
        if (!isBlank(cleaned) && !looksGenericTopic(cleaned)) {
          return cleaned;
        }
      }
    }

    return null;
  }

  private String extractFollowUpTopicReference(String originalMessage, String normalizedMessage) {
    if (isBlank(originalMessage)) {
      return null;
    }

    String candidate = originalMessage.trim();

    if (normalizedMessage.startsWith("y de ")) {
      candidate = originalMessage.trim().substring(5).trim();
    } else if (normalizedMessage.startsWith("de ")) {
      candidate = originalMessage.trim().substring(3).trim();
    } else if (normalizedMessage.startsWith("y sobre ")) {
      candidate = originalMessage.trim().substring(8).trim();
    } else if (normalizedMessage.startsWith("sobre ")) {
      candidate = originalMessage.trim().substring(6).trim();
    } else if (normalizedMessage.startsWith("y para ")) {
      candidate = originalMessage.trim().substring(7).trim();
    } else if (normalizedMessage.startsWith("para ")) {
      candidate = originalMessage.trim().substring(5).trim();
    } else if (normalizedMessage.startsWith("y con ")) {
      candidate = originalMessage.trim().substring(6).trim();
    } else if (normalizedMessage.startsWith("con ")) {
      candidate = originalMessage.trim().substring(4).trim();
    } else if (normalizedMessage.startsWith("y ")) {
      candidate = originalMessage.trim().substring(2).trim();
    }

    candidate = cleanTopicReference(candidate);

    if (isBlank(candidate) || looksGenericTopic(candidate)) {
      return null;
    }

    return candidate;
  }



  private boolean looksGenericTopic(String value) {
    if (isBlank(value)) {
      return true;
    }

    List<String> keywords = extractSearchKeywords(value);
    if (keywords.isEmpty()) {
      return true;
    }

    String normalized = normalize(value);
    return containsAny(normalized,
      "evento", "eventos", "concierto", "conciertos", "fiesta", "fiestas",
      "show", "shows", "plan", "planes", "algo", "alguna", "algun");
  }

  private String cleanTopicReference(String raw) {
    if (isBlank(raw)) {
      return null;
    }

    String cleaned = raw.trim()
      .replaceAll("(?i)\\b(en|para|cerca de|cerca a)\\s+[a-zA-Záéíóúñ\\s]+$", "")
      .replaceAll("(?i)\\b(gratis|de pago|pagado|pagada|hoy|mañana|manana|esta noche|esta tarde|esta semana)\\b.*$", "")
      .replaceAll("^[\\\"'“”]+|[\\\"'“”]+$", "")
      .trim();

    String normalized = normalize(cleaned);
    if (isBlank(cleaned) || containsAny(normalized,
      "eventos", "evento", "concierto", "conciertos", "fiesta", "fiestas", "show", "shows", "plan", "planes")) {
      return null;
    }

    return cleaned;
  }

  private List<Evento> searchEventsByReference(String reference, ConversationMemory memory) {
    String normalizedReference = normalize(reference);
    if (isBlank(normalizedReference)) {
      return List.of();
    }

    String compactReference = normalizedReference.replace(" ", "");
    List<String> keywords = extractSearchKeywords(reference);

    return eventoService.getEventosPublicos().stream()
      .filter(event -> matchesCity(event, memory.city))
      .filter(event -> {
        String text = buildEventSearchText(event);
        String compactText = text.replace(" ", "");
        int sharedKeywords = countKeywordMatches(text, keywords);
        return text.contains(normalizedReference)
          || (!compactReference.isBlank() && compactText.contains(compactReference))
          || sharedKeywords >= Math.min(2, Math.max(1, keywords.size()))
          || (keywords.size() == 1 && sharedKeywords >= 1);
      })
      .sorted(Comparator.comparingDouble(event -> scoreSpecificReference(event, normalizedReference, keywords, memory)))
      .limit(6)
      .toList();
  }

  private double scoreSpecificReference(Evento event, String normalizedReference, List<String> keywords, ConversationMemory memory) {
    String title = normalize(event.getTitulo());
    String description = normalize(event.getDescripcion());
    String text = buildEventSearchText(event);

    double score = distanceScoreForNavigation(event, memory);

    if (title.contains(normalizedReference)) {
      score -= 80;
    }

    if (description.contains(normalizedReference)) {
      score -= 45;
    }

    score -= countKeywordMatches(text, keywords) * 18.0;
    score += Math.abs(title.length() - normalizedReference.length()) / 12.0;
    return score;
  }

  private Evento findBestEventByReference(String reference, ConversationMemory memory) {
    return searchEventsByReference(reference, memory).stream().findFirst().orElse(null);
  }

  private int countKeywordMatches(String text, List<String> keywords) {
    int matches = 0;
    String normalizedText = normalize(text);
    String compactText = normalizedText.replace(" ", "");

    for (String keyword : keywords) {
      if (keyword == null || keyword.isBlank()) {
        continue;
      }

      String normalizedKeyword = normalize(keyword);
      String compactKeyword = normalizedKeyword.replace(" ", "");

      if (normalizedText.contains(normalizedKeyword)
        || (!compactKeyword.isBlank() && compactText.contains(compactKeyword))) {
        matches++;
      }
    }
    return matches;
  }

  private boolean isSpecificTopicEventIntent(String message) {
    return containsAny(message,
      "hay un concierto de", "hay concierto de", "hay algun concierto de", "hay algún concierto de",
      "hay un evento de", "hay evento de", "hay algun evento de", "hay algún evento de",
      "hay algo de", "hay algo sobre", "quiero algo de", "quiero algo sobre",
      "hay algun show de", "hay algún show de", "hay algo con",
      "hay una", "hay algun", "hay algún", "existe una", "existe algun", "existe algún",
      "tienen una", "tienen algun", "tienen algún", "tienen algo de", "tienen algo sobre",
      "busca", "búscame", "muestrame", "muéstrame", "mostrame", "ensename", "enséñame",
      "traeme", "tráeme", "consigueme", "consígueme", "tirame", "tírame", "ponme", "lanzame", "lánzame",
      "recomiendame algo de", "recomiéndame algo de", "recomendame algo de", "pasame algo de", "pásame algo de");
  }

  private AssistantResponse handleRandomNearbyEventRequest(String message, ConversationMemory memory) {
    if (!isRandomNearbyEventIntent(message)) {
      return null;
    }

    List<Evento> source = eventoService.getEventosPublicos().stream()
      .filter(event -> event.getIdEvento() != null)
      .filter(event -> !isBlank(event.getTitulo()))
      .toList();

    if (source.isEmpty()) {
      return simpleReply("No encontré eventos públicos disponibles en este momento.");
    }

    List<Evento> candidates;

    if (memory.referenceLat != null && memory.referenceLng != null) {
      candidates = source.stream()
        .filter(event -> event.getLatitud() != null && event.getLongitud() != null)
        .sorted(Comparator.comparingDouble(event ->
          haversineKm(memory.referenceLat, memory.referenceLng, event.getLatitud(), event.getLongitud())))
        .limit(12)
        .toList();

      if (candidates.isEmpty()) {
        candidates = source.stream().limit(12).toList();
      }
    } else if (!isBlank(memory.city)) {
      candidates = source.stream()
        .filter(event -> matchesCity(event, memory.city))
        .limit(12)
        .toList();

      if (candidates.isEmpty()) {
        candidates = source.stream().limit(12).toList();
      }
    } else {
      candidates = source.stream().limit(12).toList();
    }

    Evento chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    memory.resetGuided();

    return new AssistantResponse(
      "Te llevo a un evento al azar" +
        (!isBlank(chosen.getCiudad()) ? " en " + safe(chosen.getCiudad()) : "") +
        ": \"" + safe(chosen.getTitulo()) + "\".",
      List.of(new AssistantActionDto("navigate", "/event/" + chosen.getIdEvento(), null, null, null)),
      List.of()
    );
  }

  private Evento findBestEventByTitle(String reference) {
    String normalizedReference = normalize(reference);
    if (isBlank(normalizedReference)) {
      return null;
    }

    return eventoService.getEventosPublicos().stream()
      .filter(event -> !isBlank(event.getTitulo()))
      .filter(event -> {
        String title = normalize(event.getTitulo());
        return title.contains(normalizedReference)
          || normalizedReference.contains(title)
          || countSharedTokens(title, normalizedReference) >= 2;
      })
      .min(Comparator.comparingInt(event -> Math.abs(normalize(event.getTitulo()).length() - normalizedReference.length())))
      .orElse(null);
  }

  private int countSharedTokens(String left, String right) {
    LinkedHashSet<String> tokens = new LinkedHashSet<>();
    for (String token : left.split("[^a-z0-9ñ]+")) {
      if (!token.isBlank()) {
        tokens.add(token);
      }
    }

    int matches = 0;
    for (String token : right.split("[^a-z0-9ñ]+")) {
      if (!token.isBlank() && tokens.contains(token)) {
        matches++;
      }
    }

    return matches;
  }
  private Set<String> resolvePreferredCategories(Principal principal, ConversationMemory memory) {
    Set<String> preferredCategories = new LinkedHashSet<>();

    if (!memory.usePreferenceCategories) {
      return preferredCategories;
    }

    Usuario usuario = getAuthenticatedUser(principal);
    if (usuario != null && usuario.getCategoriasPreferidas() != null) {
      for (String category : usuario.getCategoriasPreferidas()) {
        preferredCategories.add(normalize(category));
      }
    }

    return preferredCategories;
  }

  private List<Evento> searchStrictEvents(ConversationMemory memory, Principal principal) {
    List<Evento> publicEvents = eventoService.getEventosPublicos();
    Set<String> preferredCategories = resolvePreferredCategories(principal, memory);

    List<Evento> exact = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      memory.zone,
      memory.targetHour,
      memory.wantsFree,
      memory.budgetMax,
      memory.intentQuery
    );

    return sortEvents(exact, memory, preferredCategories);
  }

  private List<Evento> searchEvents(ConversationMemory memory, Principal principal) {
    List<Evento> publicEvents = eventoService.getEventosPublicos();
    Set<String> preferredCategories = resolvePreferredCategories(principal, memory);

    List<Evento> exact = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      memory.zone,
      memory.targetHour,
      memory.wantsFree,
      memory.budgetMax,
      memory.intentQuery
    );
    if (!exact.isEmpty()) return sortEvents(exact, memory, preferredCategories);

    List<Evento> noZone = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      null,
      memory.targetHour,
      memory.wantsFree,
      memory.budgetMax,
      memory.intentQuery
    );
    if (!noZone.isEmpty()) return sortEvents(noZone, memory, preferredCategories);

    List<Evento> noHour = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      null,
      null,
      memory.wantsFree,
      memory.budgetMax,
      memory.intentQuery
    );
    if (!noHour.isEmpty()) return sortEvents(noHour, memory, preferredCategories);

    List<Evento> noBudget = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      null,
      null,
      memory.wantsFree,
      null,
      memory.intentQuery
    );
    if (!noBudget.isEmpty()) return sortEvents(noBudget, memory, preferredCategories);

    List<Evento> cityOnly = filterEvents(
      publicEvents,
      null,
      preferredCategories,
      memory.city,
      null,
      null,
      null,
      null,
      memory.intentQuery
    );
    if (!isBlank(memory.category) || !isBlank(memory.intentQuery) || !isBlank(memory.city)) {
      return List.of();
    }

    return sortEvents(publicEvents, memory, preferredCategories);
  }

  private List<Evento> findAlternativeEvents(ConversationMemory memory, Principal principal) {
    List<Evento> publicEvents = eventoService.getEventosPublicos();
    Set<String> preferredCategories = resolvePreferredCategories(principal, memory);

    List<Evento> relaxedPriceAndFree = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      memory.zone,
      memory.targetHour,
      null,
      null,
      memory.intentQuery
    );
    if (!relaxedPriceAndFree.isEmpty()) return sortEvents(relaxedPriceAndFree, memory, preferredCategories);

    List<Evento> relaxedZoneHour = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      memory.city,
      null,
      null,
      null,
      null,
      memory.intentQuery
    );
    if (!relaxedZoneHour.isEmpty()) return sortEvents(relaxedZoneHour, memory, preferredCategories);

    List<Evento> categoryOrText = filterEvents(
      publicEvents,
      memory.category,
      preferredCategories,
      null,
      null,
      null,
      null,
      null,
      memory.intentQuery
    );
    if (!categoryOrText.isEmpty()) return sortEvents(categoryOrText, memory, preferredCategories);

    List<Evento> cityOnly = filterEvents(
      publicEvents,
      null,
      preferredCategories,
      memory.city,
      null,
      null,
      null,
      null,
      memory.intentQuery
    );
    if (!cityOnly.isEmpty()) return sortEvents(cityOnly, memory, preferredCategories);

    if (!isBlank(memory.category) || !isBlank(memory.intentQuery) || !isBlank(memory.city)) {
      return List.of();
    }

    return sortEvents(publicEvents, memory, preferredCategories);
  }

  private List<Evento> filterEvents(
    List<Evento> source,
    String category,
    Set<String> preferredCategories,
    String city,
    String zone,
    LocalTime hour,
    Boolean wantsFree,
    Integer budgetMax,
    String intentQuery
  ) {
    return source.stream()
      .filter(event -> matchesCategory(event, category, preferredCategories))
      .filter(event -> matchesCity(event, city))
      .filter(event -> matchesZone(event, zone))
      .filter(event -> matchesHour(event, hour))
      .filter(event -> matchesFreeOrPaid(event, wantsFree))
      .filter(event -> matchesBudget(event, budgetMax))
      .limit(30)
      .toList();
  }

  private List<Evento> sortEvents(List<Evento> source, ConversationMemory memory, Set<String> preferredCategories) {
    return source.stream()
      .sorted(Comparator.comparingDouble(event -> scoreEvent(event, memory, preferredCategories)))
      .limit(6)
      .toList();
  }

  private AssistantResponse buildAvailableEventsResponse(Principal principal, ConversationMemory memory) {
    if (!isBlank(memory.intentQuery)) {
      memory.lastTopicQuery = memory.intentQuery;
    }

    List<Evento> results = searchEvents(memory, principal);

    if (results.isEmpty()) {
      rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
      return simpleReply("No encontré eventos disponibles en este momento.");
    }

    List<Evento> top = results.stream().limit(5).toList();
    List<AssistantOptionDto> options = buildOptions(top, memory, 5);

    StringBuilder reply = new StringBuilder("Estos son algunos eventos disponibles");

    if (!isBlank(memory.city)) {
      reply.append(" en ").append(memory.city);
    } else if (memory.referenceLat != null && memory.referenceLng != null) {
      reply.append(" cerca de ti");
    }

    reply.append(": ");

    for (int i = 0; i < top.size(); i++) {
      reply.append(i + 1)
        .append(". ")
        .append(formatEventSummary(top.get(i), memory))
        .append(" ");
    }

    reply.append("Si quieres, dime \"llévame a ")
      .append(safe(top.get(0).getTitulo()))
      .append("\" o toca una de las opciones.");

    rememberDiscoveryContext(memory, DiscoveryContext.AVAILABILITY);
    memory.resetGuided();
    return new AssistantResponse(reply.toString().trim(), List.of(), options);
  }

  private String formatEventSummary(Evento event, ConversationMemory memory) {
    List<String> parts = new ArrayList<>();
    parts.add(safe(event.getTitulo()));

    if (!isBlank(event.getCiudad())) {
      parts.add(safe(event.getCiudad()));
    }

    if (event.getHoraInicio() != null) {
      parts.add(event.getHoraInicio().format(HOUR_FORMAT));
    }

    if (Boolean.TRUE.equals(event.getEventoGratuito())) {
      parts.add("Gratis");
    } else if (event.getPrecio() != null) {
      parts.add("$" + event.getPrecio().toPlainString());
    }

    if (memory.referenceLat != null && memory.referenceLng != null && event.getLatitud() != null && event.getLongitud() != null) {
      double km = haversineKm(memory.referenceLat, memory.referenceLng, event.getLatitud(), event.getLongitud());
      parts.add(String.format(Locale.US, "%.1f km", km));
    }

    return String.join(" · ", parts);
  }

  private AssistantResponse askFreeOrPaid() {
    return new AssistantResponse(
      "Antes de recomendarte algo, dime si lo prefieres gratis o de pago.",
      List.of(),
      List.of(
        new AssistantOptionDto("free", "Gratis", "lo quiero gratis"),
        new AssistantOptionDto("paid", "De pago", "lo quiero de pago")
      )
    );
  }

  private AssistantResponse askBudget() {
    return new AssistantResponse(
      "Perfecto. Como es de pago, dime más o menos qué presupuesto tienes.",
      List.of(),
      List.of(
        new AssistantOptionDto("budget-20", "Hasta 20.000", "mi presupuesto es 20000"),
        new AssistantOptionDto("budget-50", "Hasta 50.000", "mi presupuesto es 50000"),
        new AssistantOptionDto("budget-100", "Hasta 100.000", "mi presupuesto es 100000")
      )
    );
  }

  private AssistantResponse askCity() {
    return new AssistantResponse(
      "¿En qué ciudad quieres el evento o el plan? Puedes elegir una opción o escribirme cualquier ciudad de Colombia.",
      List.of(),
      List.of(
        new AssistantOptionDto("city-armenia", "Armenia", "en Armenia"),
        new AssistantOptionDto("city-pereira", "Pereira", "en Pereira"),
        new AssistantOptionDto("city-medellin", "Medellín", "en Medellín")
      )
    );
  }

  private String buildPlanReply(
    Evento event,
    List<RadarPlace> nearbyPlaces,
    ClimaResponse weather,
    ConversationMemory memory
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("Te armé un plan con esta opción principal: ");
    sb.append(safe(event.getTitulo()));

    if (!isBlank(event.getCiudad())) {
      sb.append(" en ").append(safe(event.getCiudad()));
    }

    if (event.getHoraInicio() != null) {
      sb.append(" a las ").append(event.getHoraInicio().format(HOUR_FORMAT));
    }

    sb.append(".\n\n");

    if (!isBlank(event.getNombreLugar())) {
      sb.append("Lugar: ").append(safe(event.getNombreLugar())).append(".\n");
    } else if (!isBlank(event.getUbicacion())) {
      sb.append("Lugar: ").append(safe(event.getUbicacion())).append(".\n");
    }

    if (!isBlank(event.getDireccionCompleta())) {
      sb.append("Dirección: ").append(safe(event.getDireccionCompleta())).append(".\n");
    }

    if (Boolean.TRUE.equals(event.getEventoGratuito())) {
      sb.append("Costo aproximado: gratis.\n");
    } else if (event.getPrecio() != null) {
      sb.append("Costo aproximado: $").append(event.getPrecio().toPlainString()).append(".\n");
    }

    if (weather != null) {
      ClimaPronosticoDia forecast = findForecastForEvent(weather, event);
      if (forecast != null) {
        double temp = (forecast.temperaturaMaxC() + forecast.temperaturaMinC()) / 2.0;
        sb.append("Clima estimado: ")
          .append(Math.round(temp))
          .append("°C en ")
          .append(weather.ciudad())
          .append(".\n");
      } else {
        sb.append("Clima estimado: ")
          .append(Math.round(weather.temperaturaC()))
          .append("°C en ")
          .append(weather.ciudad())
          .append(".\n");
      }
    }

    if (memory.referenceLat != null && memory.referenceLng != null && event.getLatitud() != null && event.getLongitud() != null) {
      double km = haversineKm(memory.referenceLat, memory.referenceLng, event.getLatitud(), event.getLongitud());
      sb.append("Distancia aproximada desde tu ubicación: ")
        .append(String.format(Locale.US, "%.1f", km))
        .append(" km.\n");
    }

    if (Boolean.TRUE.equals(memory.needsTransport)) {
      sb.append("\nTransporte sugerido:\n");
      sb.append(buildTransportDirectoryReply(event.getCiudad()));
      sb.append("\n");
    }

    if (Boolean.TRUE.equals(memory.wantsFoodSuggestions)) {
      sb.append("\nComida cercana: ");
      if (nearbyPlaces.isEmpty()) {
        sb.append("no encontré lugares cercanos confiables para sugerirte en este momento.");
      } else {
        List<String> names = nearbyPlaces.stream()
          .limit(3)
          .map(RadarPlace::nombre)
          .filter(Objects::nonNull)
          .toList();

        sb.append(String.join(", ", names));
        sb.append(". Puedes ir antes o después del evento.");
      }
      sb.append("\n");
    }

    sb.append("\n").append(buildEmergencyReply(event.getCiudad()));
    sb.append("\n\nTe dejo abajo algunas opciones parecidas por si quieres comparar antes de entrar a una.");

    return sb.toString().trim();
  }

  private List<RadarPlace> getNearbyFood(Evento event, ConversationMemory memory) {
    if (!Boolean.TRUE.equals(memory.wantsFoodSuggestions)) {
      return List.of();
    }

    if (event.getLatitud() == null || event.getLongitud() == null) {
      return List.of();
    }

    try {
      return placesService.buscarLugares(event.getLatitud(), event.getLongitud(), 1500)
        .stream()
        .filter(place -> containsAny(normalize(place.categoria()), "restaurant", "restaurante", "bar", "food", "comida"))
        .limit(3)
        .toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  private ClimaResponse getWeather(String city) {
    if (isBlank(city)) return null;

    try {
      return climaService.consultarClimaPorCiudad(city);
    } catch (Exception e) {
      return null;
    }
  }

  private String buildTransportDirectoryReply(String city) {
    String normalizedCity = normalize(city);

    if (containsAny(normalizedCity, "armenia")) {
      return """
                Puedes usar apps como inDrive, Uber, DiDi y Picap.
                En Armenia también puedes apoyarte en:
                - Radio Taxi del Quindío WhatsApp: 311 542 2222
                - Radio Taxi del Quindío pedidos: (606) 746 2222
                - Tax Páramo S.A servicio al cliente: (606) 740 2254
                - Cooperativa de Motoristas del Quindío: (606) 748 1111
                - Taxis Armenia: 314 751 1530
                - Buses Tinto: disponible en la página web
                """.trim();
    }

    return """
            Puedes usar apps como inDrive, Uber, DiDi y Picap.
            A nivel nacional también puedes apoyarte en líneas como:
            - Taxis Libres WhatsApp: 310 211 1111
            - Taxis Libres Bogotá: (601) 311 1111 / (601) 211 1111
            - Taxis Libres Cali: (602) 444 4444
            - Taxis Libres Medellín: (604) 311 1111
            """.trim();
  }

  private String buildEmergencyReply(String city) {
    String normalizedCity = normalize(city);

    if (containsAny(normalizedCity, "armenia")) {
      return """
                Y recuerda: si ocurre una emergencia, puedes llamar a:
                - Línea de emergencias: 123
                - Policía Armenia: 123 / 112
                - Bomberos Armenia: 119
                - Ambulancias / urgencias: 123
                """.trim();
    }

    return """
            Y recuerda: si ocurre una emergencia, puedes llamar a:
            - Línea única de emergencias: 123
            - Policía Nacional: 112
            - Bomberos: 119
            - Cruz Roja: 132
            - Defensa Civil: 144
            """.trim();
  }

  private AssistantOptionDto toOption(Evento evento, ConversationMemory memory) {
    if (evento != null && evento.getIdEvento() != null) {
      memory.lastFocusedEventId = evento.getIdEvento();
    }

    String city = safe(evento.getCiudad());
    String hour = evento.getHoraInicio() != null ? evento.getHoraInicio().format(HOUR_FORMAT) : "";
    String price = "";

    if (Boolean.TRUE.equals(evento.getEventoGratuito())) {
      price = "Gratis";
    } else if (evento.getPrecio() != null) {
      price = "$" + evento.getPrecio().toPlainString();
    }

    String distance = "";
    if (memory.referenceLat != null && memory.referenceLng != null && evento.getLatitud() != null && evento.getLongitud() != null) {
      double km = haversineKm(memory.referenceLat, memory.referenceLng, evento.getLatitud(), evento.getLongitud());
      distance = String.format(Locale.US, "%.1f km", km);
    }

    List<String> parts = new ArrayList<>();
    parts.add(safe(evento.getTitulo()));

    if (!city.isBlank()) parts.add(city);
    if (!hour.isBlank()) parts.add(hour);
    if (!price.isBlank()) parts.add(price);
    if (!distance.isBlank()) parts.add(distance);

    return new AssistantOptionDto(
      String.valueOf(evento.getIdEvento()),
      String.join(" · ", parts),
      "abrir evento " + evento.getIdEvento()
    );
  }

  private void mergePageContext(AssistantRequest request, ConversationMemory memory) {
    if (request == null || request.pageContext() == null) return;

    Map<String, Object> context = request.pageContext();

    String currentView = safeString(context.get("currentView"));
    if (!currentView.isBlank()) {
      memory.lastRoute = currentView;
    }

    Double userLat = toDouble(context.get("userLat"));
    Double userLng = toDouble(context.get("userLng"));
    Double mapCenterLat = toDouble(context.get("mapCenterLat"));
    Double mapCenterLng = toDouble(context.get("mapCenterLng"));

    if (userLat != null && userLng != null) {
      memory.referenceLat = userLat;
      memory.referenceLng = userLng;
    } else if (mapCenterLat != null && mapCenterLng != null) {
      memory.referenceLat = mapCenterLat;
      memory.referenceLng = mapCenterLng;
    }

    Integer currentEventId = toInteger(context.get("currentEventId"));
    if (currentEventId != null) {
      memory.currentEventId = currentEventId;
      memory.currentEventTitle = safeString(context.get("currentEventTitle"));
      memory.currentEventCity = safeString(context.get("currentEventCity"));
      memory.currentEventCategory = safeString(context.get("currentEventCategory"));
      memory.currentEventPrice = toInteger(context.get("currentEventPrice"));
      memory.currentEventIsFree = toBoolean(context.get("currentEventIsFree"));
      memory.lastFocusedEventId = currentEventId;
    } else if (!currentView.startsWith("/event/")) {
      clearCurrentEventContext(memory);
    }

    List<Integer> visibleEventIds = toIntegerList(context.get("visibleEventIds"));
    if (!visibleEventIds.isEmpty()) {
      memory.lastShownEventIds = visibleEventIds.stream().distinct().limit(10).toList();
      memory.lastShownEventTitles = toStringList(context.get("visibleEventTitles"))
        .stream()
        .limit(10)
        .toList();
    }

    Object activeFiltersRaw = context.get("activeFilters");
    if (activeFiltersRaw instanceof Map<?, ?> activeFilters) {
      String category = safeString(activeFilters.get("category"));
      if (!category.isBlank()) {
        memory.category = category;
      }

      String searchText = safeString(activeFilters.get("searchText"));
      if (!searchText.isBlank()) {
        memory.intentQuery = searchText;
      }
    }
  }

  private AssistantResponse handleCurrentEventQuestion(String normalizedMessage, ConversationMemory memory) {
    if (memory.currentEventId == null || !isCurrentEventQuestion(normalizedMessage, memory)) {
      return null;
    }

    String title = !isBlank(memory.currentEventTitle) ? memory.currentEventTitle : "este evento";

    if (containsAny(normalizedMessage, "gratis", "cuesta", "vale", "precio", "pagar")) {
      if (Boolean.TRUE.equals(memory.currentEventIsFree)) {
        return simpleReply("Sí. \"" + title + "\" es gratis.");
      }

      if (memory.currentEventPrice != null) {
        return simpleReply("No es gratis. \"" + title + "\" cuesta " + formatCop(memory.currentEventPrice) + ".");
      }

      return simpleReply("Ese evento no aparece como gratis. Revísalo en el detalle para confirmar el valor.");
    }

    if (containsAny(normalizedMessage, "donde", "dónde", "queda", "ubicado", "ubicacion", "ubicación", "ciudad")) {
      if (!isBlank(memory.currentEventCity)) {
        return simpleReply("\"" + title + "\" está en " + memory.currentEventCity + ".");
      }

      return simpleReply("No tengo la ciudad de ese evento en el contexto actual, pero ya estás en su detalle.");
    }

    if (containsAny(normalizedMessage, "categoria", "categoría", "tipo", "de que trata", "de qué trata")) {
      if (!isBlank(memory.currentEventCategory)) {
        return simpleReply("\"" + title + "\" es de categoría " + memory.currentEventCategory + ".");
      }

      return simpleReply("No tengo la categoría exacta en este momento, pero ya estás en el detalle del evento.");
    }

    if (containsAny(normalizedMessage, "comentarios", "comentario")) {
      return new AssistantResponse(
        "Sí. Si el evento tiene comentarios habilitados, te bajo a esa sección ahora.",
        List.of(new AssistantActionDto("scroll", null, "comments-section", null, null)),
        List.of()
      );
    }

    if (containsAny(normalizedMessage, "inscrib", "registr", "unirme", "meterme")) {
      return simpleReply(
        "Si te interesa \"" + title + "\", desde su detalle puedes inscribirte o iniciar el pago si aplica. "
          + "Si no ves el botón, revisa que tengas sesión iniciada."
      );
    }

    StringBuilder reply = new StringBuilder("Estás viendo \"" + title + "\"");

    if (!isBlank(memory.currentEventCity)) {
      reply.append(" en ").append(memory.currentEventCity);
    }

    if (!isBlank(memory.currentEventCategory)) {
      reply.append(". Es un evento de ").append(memory.currentEventCategory);
    }

    if (Boolean.TRUE.equals(memory.currentEventIsFree)) {
      reply.append(" y además es gratis");
    } else if (memory.currentEventPrice != null) {
      reply.append(" y cuesta ").append(formatCop(memory.currentEventPrice));
    }

    reply.append(".");
    return simpleReply(reply.toString());
  }

  private boolean isCurrentEventQuestion(String normalizedMessage, ConversationMemory memory) {
    boolean asksEventAttribute = containsAny(normalizedMessage,
      "gratis", "cuesta", "vale", "precio", "pagar",
      "donde", "dónde", "queda", "ubicado", "ubicacion", "ubicación", "ciudad",
      "categoria", "categoría", "tipo", "de que trata", "de qué trata",
      "comentarios", "comentario",
      "inscrib", "registr", "unirme", "meterme");

    if (!asksEventAttribute) {
      return false;
    }

    if (safe(memory.lastRoute).startsWith("/event/")) {
      return true;
    }

    return containsAny(normalizedMessage,
      "este evento", "este plan", "de este evento", "del evento", "aqui", "aquí");
  }

  private void mergeHistoryContext(AssistantRequest request, ConversationMemory memory) {
    if (request == null || request.history() == null || request.history().isEmpty()) {
      return;
    }

    AssistantChatMessageDto lastUser = null;
    AssistantChatMessageDto lastAssistant = null;

    for (AssistantChatMessageDto item : request.history()) {
      if (item == null || isBlank(item.text())) continue;

      if ("user".equalsIgnoreCase(safe(item.role()))) {
        lastUser = item;
      } else if ("assistant".equalsIgnoreCase(safe(item.role()))) {
        lastAssistant = item;
      }
    }

    if (lastUser != null) {
      memory.lastUserMessage = safe(lastUser.text());
    }

    if (lastAssistant != null) {
      memory.lastAssistantMessage = safe(lastAssistant.text());
    }

    if (isBlank(memory.lastTopicQuery) && !isBlank(memory.lastUserMessage)) {
      memory.lastTopicQuery = memory.lastUserMessage;
    }
  }

  private AssistantResponse handleResultListFollowUp(String normalizedMessage, ConversationMemory memory) {
    List<Evento> shownEvents = resolveLastShownEvents(memory);
    if (shownEvents.isEmpty()) {
      return null;
    }

    Integer ordinal = extractOrdinalReference(normalizedMessage);

    boolean asksOther = containsAny(normalizedMessage,
      "otro", "otra", "otra opcion", "otra opción");
    boolean asksCheapest = containsAny(normalizedMessage,
      "mas barato", "más barato", "mas economico", "más económico", "economico", "económico");
    boolean asksNearest = containsAny(normalizedMessage,
      "mas cerca", "más cerca", "mas cercano", "más cercano", "cercano");
    boolean asksToOpen = containsAny(normalizedMessage,
      "abre", "abrime", "abreme", "muestrame", "muéstrame", "mostrame",
      "llevame", "llévame", "entra", "quiero ese", "quiero esa");

    if (ordinal == null && !asksOther && !asksCheapest && !asksNearest) {
      return null;
    }

    if (ordinal != null) {
      int index = ordinal - 1;
      if (index < 0 || index >= shownEvents.size()) {
        return simpleReply("Solo tengo " + shownEvents.size() + " opciones recientes para escoger.");
      }

      Evento selected = shownEvents.get(index);
      memory.lastFocusedEventId = selected.getIdEvento();

      return new AssistantResponse(
        "Listo. Te llevo a la opción " + ordinal + ": \"" + safe(selected.getTitulo()) + "\".",
        List.of(new AssistantActionDto("navigate", "/event/" + selected.getIdEvento(), null, null, null)),
        List.of()
      );
    }

    List<Evento> reordered = new ArrayList<>(shownEvents);

    if (asksCheapest) {
      reordered = reordered.stream()
        .sorted(Comparator
          .comparing((Evento event) -> comparablePrice(event))
          .thenComparingDouble((Evento event) -> distanceScoreForNavigation(event, memory)))
        .toList();
    }

    if (asksNearest) {
      if (memory.referenceLat == null || memory.referenceLng == null) {
        return simpleReply("Para decirte cuál queda más cerca, necesito tu ubicación o que abras el mapa para ubicarte mejor.");
      }

      reordered = reordered.stream()
        .sorted(Comparator
          .comparingDouble((Evento event) -> distanceScoreForNavigation(event, memory))
          .thenComparing((Evento event) -> comparablePrice(event)))
        .toList();
    }

    if (asksOther) {
      Integer focusedId = memory.lastFocusedEventId != null
        ? memory.lastFocusedEventId
        : memory.currentEventId;

      Evento alternative = reordered.stream()
        .filter(event -> !Objects.equals(event.getIdEvento(), focusedId))
        .findFirst()
        .orElse(reordered.get(0));

      reordered = moveEventToFront(reordered, alternative.getIdEvento());
    }

    Evento top = reordered.get(0);
    memory.lastFocusedEventId = top.getIdEvento();

    List<AssistantOptionDto> options = buildOptions(reordered, memory, 5);

    if (asksToOpen && asksOther && !asksCheapest && !asksNearest) {
      return new AssistantResponse(
        "Claro. Te llevo a otra opción: \"" + safe(top.getTitulo()) + "\".",
        List.of(new AssistantActionDto("navigate", "/event/" + top.getIdEvento(), null, null, null)),
        List.of()
      );
    }

    String reply;
    if (asksCheapest) {
      reply = "Listo. Reordené las opciones por precio. La más barata ahora es \"" + safe(top.getTitulo()) + "\".";
    } else if (asksNearest) {
      reply = "Listo. Reordené las opciones por cercanía. La que te queda más cerca ahora es \"" + safe(top.getTitulo()) + "\".";
    } else {
      reply = "Claro. Te dejo otra alternativa: \"" + safe(top.getTitulo()) + "\".";
    }

    return new AssistantResponse(reply, List.of(), options);
  }

  private List<Evento> resolveLastShownEvents(ConversationMemory memory) {
    if (memory.lastShownEventIds == null || memory.lastShownEventIds.isEmpty()) {
      return List.of();
    }

    List<Integer> orderedIds = new ArrayList<>(memory.lastShownEventIds);

    return eventoService.getEventosPublicos().stream()
      .filter(event -> event.getIdEvento() != null && orderedIds.contains(event.getIdEvento()))
      .sorted(Comparator.comparingInt(event -> orderedIds.indexOf(event.getIdEvento())))
      .toList();
  }

  private Integer extractOrdinalReference(String normalizedMessage) {
    if (isBlank(normalizedMessage)) return null;

    if (containsAny(normalizedMessage, "primer", "primero", "primera")) return 1;
    if (containsAny(normalizedMessage, "segundo", "segunda")) return 2;
    if (containsAny(normalizedMessage, "tercer", "tercero", "tercera")) return 3;
    if (containsAny(normalizedMessage, "cuarto", "cuarta")) return 4;
    if (containsAny(normalizedMessage, "quinto", "quinta")) return 5;

    Matcher matcher = Pattern.compile("\\b([1-5])\\b").matcher(normalizedMessage);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }

    return null;
  }

  private List<Evento> moveEventToFront(List<Evento> source, Integer targetId) {
    if (source == null || source.isEmpty() || targetId == null) {
      return source == null ? List.of() : source;
    }

    List<Evento> reordered = new ArrayList<>(source);
    Evento selected = null;

    for (Evento event : reordered) {
      if (Objects.equals(event.getIdEvento(), targetId)) {
        selected = event;
        break;
      }
    }

    if (selected == null) {
      return reordered;
    }

    reordered.remove(selected);
    reordered.add(0, selected);
    return reordered;
  }

  private List<AssistantOptionDto> buildOptions(List<Evento> results, ConversationMemory memory, int limit) {
    List<Evento> safeResults = results == null
      ? List.of()
      : results.stream().filter(Objects::nonNull).toList();

    rememberResultSet(memory, safeResults);

    return safeResults.stream()
      .limit(limit)
      .map(event -> toOption(event, memory))
      .toList();
  }

  private void rememberResultSet(ConversationMemory memory, List<Evento> results) {
    if (memory == null || results == null || results.isEmpty()) {
      return;
    }

    List<Evento> cleaned = results.stream()
      .filter(Objects::nonNull)
      .filter(event -> event.getIdEvento() != null)
      .toList();

    if (cleaned.isEmpty()) {
      return;
    }

    memory.lastShownEventIds = cleaned.stream()
      .map(Evento::getIdEvento)
      .distinct()
      .limit(10)
      .toList();

    memory.lastShownEventTitles = cleaned.stream()
      .map(Evento::getTitulo)
      .filter(Objects::nonNull)
      .limit(10)
      .toList();

    if (memory.lastFocusedEventId == null) {
      memory.lastFocusedEventId = cleaned.get(0).getIdEvento();
    }
  }

  private void clearCurrentEventContext(ConversationMemory memory) {
    memory.currentEventId = null;
    memory.currentEventTitle = null;
    memory.currentEventCity = null;
    memory.currentEventCategory = null;
    memory.currentEventPrice = null;
    memory.currentEventIsFree = null;
  }

  private String safeString(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private Integer toInteger(Object value) {
    if (value == null) return null;

    if (value instanceof Number number) {
      return number.intValue();
    }

    try {
      String digits = String.valueOf(value).replaceAll("[^\\d-]", "");
      if (digits.isBlank()) return null;
      return Integer.parseInt(digits);
    } catch (Exception e) {
      return null;
    }
  }

  private Boolean toBoolean(Object value) {
    if (value == null) return null;
    if (value instanceof Boolean b) return b;

    String text = normalize(String.valueOf(value));
    if ("true".equals(text) || "si".equals(text) || "sí".equals(text) || "1".equals(text)) return true;
    if ("false".equals(text) || "no".equals(text) || "0".equals(text)) return false;

    return null;
  }

  private List<Integer> toIntegerList(Object value) {
    if (!(value instanceof List<?> list)) return List.of();

    List<Integer> result = new ArrayList<>();
    for (Object item : list) {
      Integer parsed = toInteger(item);
      if (parsed != null && !result.contains(parsed)) {
        result.add(parsed);
      }
    }
    return result;
  }

  private List<String> toStringList(Object value) {
    if (!(value instanceof List<?> list)) return List.of();

    List<String> result = new ArrayList<>();
    for (Object item : list) {
      String parsed = safeString(item);
      if (!parsed.isBlank()) {
        result.add(parsed);
      }
    }
    return result;
  }

  private BigDecimal comparablePrice(Evento event) {
    if (event == null || Boolean.TRUE.equals(event.getEventoGratuito()) || event.getPrecio() == null) {
      return BigDecimal.ZERO;
    }
    return event.getPrecio();
  }

  private String formatCop(Integer value) {
    if (value == null) return "sin precio definido";
    return "$" + String.format(Locale.US, "%,d", value).replace(',', '.');
  }

  private void applyAwaitingAnswer(String message, ConversationMemory memory) {
    if (memory.awaiting == Awaiting.NONE || isBlank(message)) {
      return;
    }

    String normalized = normalize(message);

    switch (memory.awaiting) {
      case FREE_OR_PAID -> {
        Integer budget = detectBudget(message);

        if (budget != null) {
          memory.wantsFree = false;
          memory.budgetMax = budget;
          memory.awaiting = Awaiting.NONE;
        } else if (containsAny(normalized, "gratis", "gratuito", "gratuita", "sin pagar")) {
          memory.wantsFree = true;
          memory.awaiting = Awaiting.NONE;
        } else if (containsAny(normalized, "de pago", "pago", "pagado", "pagada", "con presupuesto")) {
          memory.wantsFree = false;
          memory.awaiting = Awaiting.NONE;
        }
      }
      case BUDGET -> {
        Integer budget = detectBudget(message);
        if (budget != null) {
          memory.budgetMax = budget;
          memory.awaiting = Awaiting.NONE;
        }
      }
      case CITY -> {
        String city = detectCity(message);
        if (!isBlank(city)) {
          memory.city = city;
          memory.awaiting = Awaiting.NONE;
        }
      }
      case CATEGORY -> {
        String category = detectCategory(message);
        if (!isBlank(category)) {
          memory.category = category;
          memory.awaiting = Awaiting.NONE;
        }
      }
      case TRANSPORT -> {
        Boolean answer = detectTransportPreference(message);
        if (answer == null) {
          answer = detectYesNo(normalized);
        }
        if (answer != null) {
          memory.needsTransport = answer;
          memory.awaiting = Awaiting.NONE;
        }
      }
      case FOOD -> {
        Boolean answer = detectFoodPreference(message);
        if (answer == null) {
          answer = detectYesNo(normalized);
        }
        if (answer != null) {
          memory.wantsFoodSuggestions = answer;
          memory.awaiting = Awaiting.NONE;
        }
      }
      default -> {
      }
    }
  }

  private void mergeDetectedContext(String message, ConversationMemory memory) {
    String category = detectCategory(message);
    if (!isBlank(category)) {
      memory.category = category;
    }

    List<String> cities = detectCities(message);
    if (!cities.isEmpty()) {
      memory.city = cities.get(cities.size() - 1);
    }

    String zone = detectZone(message);
    if (!isBlank(zone)) {
      memory.zone = zone;
    }

    LocalTime hour = detectHour(message);
    if (hour != null) {
      memory.targetHour = hour;
    }

    Integer budget = detectBudget(message);
    if (budget != null) {
      memory.budgetMax = budget;
      if (memory.wantsFree == null) {
        memory.wantsFree = false;
      }
    }

    String normalized = normalize(message);

    if (containsAny(normalized, "gratis", "gratuito", "gratuita", "sin pagar")) {
      memory.wantsFree = true;
    }

    if (containsAny(normalized, "de pago", "pago", "pagada", "pagado")) {
      memory.wantsFree = false;
    }

    Boolean transportPreference = detectTransportPreference(message);
    if (transportPreference != null) {
      memory.needsTransport = transportPreference;
    }

    Boolean foodPreference = detectFoodPreference(message);
    if (foodPreference != null) {
      memory.wantsFoodSuggestions = foodPreference;
    }
  }

  private Usuario getAuthenticatedUser(Principal principal) {
    if (principal == null || isBlank(principal.getName())) {
      return null;
    }

    try {
      return usuarioService.getUsuarioPorCorreo(principal.getName().trim());
    } catch (Exception e) {
      return null;
    }
  }

  private double scoreEvent(Evento event, ConversationMemory memory, Set<String> preferredCategories) {
    double score = 0;

    if (!isBlank(memory.category)) {
      boolean exactCategory = normalize(event.getCategoria()).equals(normalize(memory.category));
      boolean hintedCategory = eventMatchesCategoryHint(event, memory.category);

      if (exactCategory) {
        score -= 55;
      } else if (hintedCategory) {
        score -= 35;
      } else {
        score += 65;
      }
    }

    if (!preferredCategories.isEmpty() && preferredCategories.contains(normalize(event.getCategoria()))) {
      score -= 35;
    }

    if (!isBlank(memory.city) && normalize(event.getCiudad()).contains(normalize(memory.city))) {
      score -= 20;
    }

    if (!isBlank(memory.intentQuery)) {
      String text = buildEventSearchText(event);
      List<String> keywords = extractSearchKeywords(memory.intentQuery);

      if (text.contains(normalize(memory.intentQuery))) {
        score -= 26;
      }

      int matchedKeywords = countKeywordMatches(text, keywords);
      score -= matchedKeywords * 10.0;

      if (!keywords.isEmpty() && matchedKeywords == 0) {
        score += 10;
      }
    }

    if (!isBlank(memory.mood)) {
      score += moodPenalty(event, memory.mood);
    }

    if (memory.targetHour != null && event.getHoraInicio() != null) {
      int diff = Math.abs(event.getHoraInicio().toSecondOfDay() - memory.targetHour.toSecondOfDay());
      score += diff / 60.0;
    }

    if (Boolean.TRUE.equals(memory.wantsFree) && Boolean.TRUE.equals(event.getEventoGratuito())) {
      score -= 10;
    }

    if (memory.referenceLat != null && memory.referenceLng != null && event.getLatitud() != null && event.getLongitud() != null) {
      score += haversineKm(memory.referenceLat, memory.referenceLng, event.getLatitud(), event.getLongitud()) * 2.0;
    }

    return score;
  }

  private boolean buildEventSearchText(Evento event, String intentQuery, String category) {
    if (isBlank(intentQuery)) {
      return true;
    }

    String text = buildEventSearchText(event);
    List<String> keywords = extractSearchKeywords(intentQuery);

    if (!isBlank(category) && text.contains(normalize(category))) {
      return true;
    }

    if (keywords.isEmpty()) {
      return true;
    }

    return keywords.stream().anyMatch(text::contains);
  }

  private String buildEventSearchText(Evento event) {
    String categoryHints = String.join(" ", categoryKeywords(safe(event.getCategoria())));

    return normalize(
      safe(event.getTitulo()) + " " +
        safe(event.getDescripcion()) + " " +
        safe(event.getCategoria()) + " " +
        categoryHints + " " +
        safe(event.getNombreLugar()) + " " +
        safe(event.getUbicacion()) + " " +
        safe(event.getDireccionCompleta()) + " " +
        safe(event.getCiudad())
    );
  }

  private List<String> extractSearchKeywords(String raw) {
    String normalized = normalize(raw).replaceAll("[^a-z0-9ñ\\s]", " ");
    String[] tokens = normalized.split("\\s+");

    Set<String> stopWords = new HashSet<>(List.of(
      "quiero", "algo", "evento", "eventos", "plan", "planes",
      "para", "con", "sin", "gratis", "pago", "pagado", "pagada",
      "de", "del", "la", "el", "en", "por", "mi", "mis", "un",
      "una", "que", "estoy", "esta", "pero", "dia", "clima", "frio",
      "fria", "caliente", "calor", "soleado", "soleada", "lluvia",
      "lloviendo", "porfa", "entonces", "oe", "aja", "segun",
      "preferencias", "gustos", "hay", "existe", "tienen", "algun",
      "alguna", "presupuesto", "hasta", "pesos", "recomiendame",
      "recomienda", "buscame", "muestrame", "mostrame", "llevame",
      "abre", "abrime", "ir", "entrar", "ver", "hacer", "salir",
      "parche", "vuelta", "vueltica", "vaina", "cosa", "alguito",
      "tirame", "ponme", "lanzame", "traeme", "consigueme", "pasame"
    ));

    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    for (String token : tokens) {
      String comparable = toComparableToken(token);
      if (comparable.length() >= 3
        && !stopWords.contains(comparable)
        && !comparable.matches("\\d+")) {
        keywords.add(comparable);
      }
    }

    return keywords.stream().toList();
  }

  private String buildNoResultsMessage(ConversationMemory memory) {
    if (!isBlank(memory.city)) {
      return "No hay eventos disponibles en " + memory.city + " con esos requisitos en este momento.";
    }
    return "No encontré eventos disponibles con esos requisitos en este momento.";
  }

  private void rememberDiscoveryContext(ConversationMemory memory, DiscoveryContext context) {
    if (memory == null) {
      return;
    }

    memory.lastDiscoveryContext = context == null ? DiscoveryContext.NONE : context;

    if (!isBlank(memory.intentQuery)) {
      memory.lastTopicQuery = memory.intentQuery;
    }
  }

  private double distanceScoreForNavigation(Evento event, ConversationMemory memory) {
    if (memory.referenceLat != null && memory.referenceLng != null && event.getLatitud() != null && event.getLongitud() != null) {
      return haversineKm(memory.referenceLat, memory.referenceLng, event.getLatitud(), event.getLongitud());
    }
    return 0;
  }

  private AssistantResponse handleMoodRecommendationIntent(Principal principal, ConversationMemory memory, String originalMessage, String normalizedMessage) {
    if (!isMoodRecommendationIntent(normalizedMessage)) {
      return null;
    }

    memory.resetGuided();
    mergeDetectedContext(originalMessage, memory);
    memory.intentQuery = originalMessage;
    memory.mood = detectMood(normalizedMessage);

    List<Evento> results = searchEvents(memory, principal);
    if (results.isEmpty()) {
      return simpleReply(buildNoResultsMessage(memory));
    }

    List<AssistantOptionDto> options = buildOptions(results, memory, 5);

    String intro = switch (memory.mood) {
      case "TRISTE" -> "Para animarte, estas opciones te pueden caer muy bien.";
      case "FELIZ" -> "Si ya estás feliz, estas opciones te pueden ayudar a pasarla todavía mejor.";
      case "ESTRESADO" -> "Si estás estresado, estas opciones te pueden ayudar a despejarte.";
      case "ABURRIDO" -> "Si estás aburrido, estas opciones te pueden sacar de la rutina.";
      default -> "Estas opciones te pueden interesar.";
    };

    memory.resetGuided();
    return new AssistantResponse(intro, List.of(), options);
  }

  private boolean isMoodRecommendationIntent(String message) {
    return containsAny(message,
      "estoy triste", "ando triste", "me siento triste", "ando bajoneado", "ando bajoneada",
      "estoy depre", "ando depre", "estoy deprimido", "estoy deprimida", "estoy desanimado", "estoy desanimada",
      "estoy feliz", "ando feliz", "me siento feliz", "estoy contento", "estoy contenta", "estoy emocionado", "estoy emocionada",
      "estoy aburrido", "estoy aburrida", "ando aburrido", "ando aburrida", "estoy mamado", "estoy mamada", "estoy sin plan",
      "estoy estresado", "estoy estresada", "ando estresado", "ando estresada", "estoy agotado", "estoy agotada", "ando rayado", "ando rayada")
      && containsAny(message, "recomienda", "recomiendame", "recomiéndame", "que me recomiendas", "qué me recomiendas", "quiero algo", "buscame algo", "muestrame algo");
  }

  private String detectMood(String message) {
    if (containsAny(message, "triste", "desanimado", "desanimada", "depre", "deprimido", "deprimida", "bajoneado", "bajoneada")) return "TRISTE";
    if (containsAny(message, "feliz", "contento", "contenta", "emocionado", "emocionada", "animado", "animada")) return "FELIZ";
    if (containsAny(message, "estresado", "estresada", "agotado", "agotada", "mamado", "mamada", "rayado", "rayada", "abrumado", "abrumada")) return "ESTRESADO";
    if (containsAny(message, "aburrido", "aburrida", "sin plan", "desparchado", "desparchada", "sin nada que hacer")) return "ABURRIDO";
    return "NEUTRO";
  }

  private double moodPenalty(Evento event, String mood) {
    String text = buildEventSearchText(event);

    return switch (mood) {
      case "TRISTE" -> {
        if (containsAny(text, "musica", "música", "fiesta", "arte", "comedia", "gaming")) yield -12;
        yield 0;
      }
      case "FELIZ" -> {
        if (containsAny(text, "fiesta", "musica", "música", "deporte", "gaming")) yield -12;
        yield 0;
      }
      case "ESTRESADO" -> {
        if (containsAny(text, "arte", "gastronomia", "gastronomía", "cultural", "relax")) yield -12;
        yield 0;
      }
      case "ABURRIDO" -> {
        if (containsAny(text, "gaming", "fiesta", "deporte", "musica", "música")) yield -12;
        yield 0;
      }
      default -> 0;
    };
  }

  private AssistantResponse handleWeatherRecommendationIntent(Principal principal, ConversationMemory memory, String originalMessage, String normalizedMessage) {
    if (!isWeatherRecommendationIntent(normalizedMessage)) {
      return null;
    }

    memory.resetGuided();
    mergeDetectedContext(originalMessage, memory);
    memory.intentQuery = originalMessage;

    List<Evento> results = searchEvents(memory, principal);
    if (results.isEmpty()) {
      return simpleReply(buildNoResultsMessage(memory));
    }

    List<Evento> rankedByWeather = results.stream()
      .sorted(Comparator
        .comparingDouble((Evento event) -> scoreWeatherPreference(event, normalizedMessage))
        .thenComparingDouble(event -> distanceScoreForNavigation(event, memory)))
      .toList();

    Evento top = rankedByWeather.get(0);
    ClimaResponse weather = getWeather(top.getCiudad());

    List<AssistantOptionDto> options = buildOptions(rankedByWeather, memory, 5);

    String reply = buildWeatherRecommendationReply(top, weather, normalizedMessage);

    memory.resetGuided();
    return new AssistantResponse(reply, List.of(), options);
  }

  private boolean isWeatherRecommendationIntent(String message) {
    boolean hasWeatherCue = containsAny(message,
      "esta lloviendo", "está lloviendo", "si llueve", "con lluvia", "lluvia", "aguacero", "diluvio",
      "dia soleado", "día soleado", "soleado", "soleada", "sol", "solazo", "asoleado", "asoleada",
      "clima", "pronostico", "pronóstico", "tiempo",
      "frio", "fria", "fresco", "fresquito", "helado", "heladito", "frisquito",
      "caliente", "calor", "calientico", "templado", "bochorno", "pegando duro el sol", "nublado");

    boolean hasRecommendationCue = containsAny(message,
      "evento", "eventos", "recomienda", "recomiendame", "quiero", "muestrame", "buscame", "tirame algo", "ponme algo")
      || safe(message).startsWith("y ")
      || safe(message).startsWith("de ");

    return hasWeatherCue && hasRecommendationCue;
  }

  private String buildWeatherRecommendationReply(Evento event, ClimaResponse weather, String message) {
    StringBuilder sb = new StringBuilder();
    sb.append("Pensando en el clima, te recomiendo \"")
      .append(safe(event.getTitulo()))
      .append("\"");

    if (!isBlank(event.getCiudad())) {
      sb.append(" en ").append(safe(event.getCiudad()));
    }

    if (event.getFecha() != null) {
      LocalDate eventDate = event.getFecha().toLocalDate();
      String dia = eventDate.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
      sb.append(", que será el ").append(dia);
    }

    if (weather != null) {
      ClimaPronosticoDia forecast = findForecastForEvent(weather, event);
      if (forecast != null) {
        sb.append(". El pronóstico marca aproximadamente ")
          .append(Math.round(forecast.temperaturaMaxC()))
          .append("°C y ")
          .append(describeWeatherCode(forecast.codigoClima()))
          .append(".");
      } else {
        sb.append(". Ahora mismo en ")
          .append(weather.ciudad())
          .append(" hay unos ")
          .append(Math.round(weather.temperaturaC()))
          .append("°C y ")
          .append(describeWeatherCode(weather.codigoClima()))
          .append(".");
      }
    }

    if (containsAny(message, "lloviendo", "lluvia")) {
      sb.append(" Si sigue ese clima, esta opción puede funcionarte mejor que una totalmente al aire libre.");
    }

    if (containsAny(message, "soleado", "sol", "caliente", "calor", "calientico")) {
      sb.append(" Si estás buscando un ambiente más cálido, esta es de las mejores opciones que encontré.");
    }

    if (containsAny(message, "frio", "fria", "fresco", "fresquito")) {
      sb.append(" Si estás buscando un clima más fresco, esta opción encaja mejor que las más calientes.");
    }

    return sb.toString().trim();
  }

  private double scoreWeatherPreference(Evento event, String message) {
    ClimaResponse weather = getWeather(event.getCiudad());
    if (weather == null) {
      return 999;
    }

    double score = 0;
    double temp = weather.temperaturaC();
    int code = weather.codigoClima();

    if (containsAny(message, "frio", "fria", "fresco", "fresquito")) {
      score += Math.abs(temp - 18.0);
    }

    if (containsAny(message, "caliente", "calor", "calientico", "soleado", "sol")) {
      score += Math.abs(temp - 30.0);
    }

    if (containsAny(message, "lloviendo", "lluvia")) {
      score += isRainyWeatherCode(code) ? 0 : 8;
    }

    if (containsAny(message, "soleado", "sol")) {
      score += isSunnyWeatherCode(code) ? 0 : 8;
    }

    return score;
  }

  private boolean isRainyWeatherCode(int code) {
    return code == 51 || code == 53 || code == 55 || code == 61 || code == 63 || code == 65 || code == 80 || code == 81 || code == 82;
  }

  private boolean isSunnyWeatherCode(int code) {
    return code == 0 || code == 1;
  }

  private ClimaPronosticoDia findForecastForEvent(ClimaResponse weather, Evento event) {
    if (weather == null || weather.pronosticoDias() == null || event == null || event.getFecha() == null) {
      return null;
    }

    LocalDate eventDate = event.getFecha().toLocalDate();
    return weather.pronosticoDias().stream()
      .filter(day -> day.fecha().equals(eventDate.toString()))
      .findFirst()
      .orElse(null);
  }

  private String describeWeatherCode(int code) {
    if (code == 0) return "cielo despejado";
    if (code == 1 || code == 2 || code == 3) return "intervalos de nubes";
    if (code == 45 || code == 48) return "neblina";
    if (code == 51 || code == 53 || code == 55) return "llovizna";
    if (code == 61 || code == 63 || code == 65) return "lluvia";
    if (code == 71 || code == 73 || code == 75) return "nieve";
    if (code == 80 || code == 81 || code == 82) return "chubascos";
    if (code == 95 || code == 96 || code == 99) return "tormenta";
    return "clima variable";
  }

  private boolean matchesCategory(Evento event, String category, Set<String> preferredCategories) {
    if (!isBlank(category)) {
      return normalize(event.getCategoria()).equals(normalize(category))
        || eventMatchesCategoryHint(event, category);
    }

    if (!preferredCategories.isEmpty()) {
      return preferredCategories.contains(normalize(event.getCategoria()));
    }

    return true;
  }

  private boolean eventMatchesCategoryHint(Evento event, String category) {
    if (isBlank(category)) {
      return true;
    }

    String text = buildEventSearchText(event);
    for (String keyword : categoryKeywords(category)) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private List<String> categoryKeywords(String category) {
    String normalized = normalize(category);
    return switch (normalized) {
      case "gaming" -> List.of(
        "gaming", "gamer", "videojuego", "videojuegos", "esports", "e sports", "e-sports",
        "torneo gamer", "playstation", "ps5", "ps4", "xbox", "nintendo", "switch", "steam",
        "valorant", "fifa", "fc 25", "fc25", "fortnite", "league of legends", "lol", "dota",
        "lan party", "consola", "free fire", "cod", "call of duty", "warzone", "minecraft", "smash"
      );
      case "musica" -> List.of(
        "musica", "concierto", "conciertos", "show", "toque", "dj", "presentacion", "presentación",
        "banda", "artista", "festival", "karaoke", "salsa", "bachata",
        "vallenato", "reggaeton", "electronica", "techno", "house",
        "rap", "trap", "hip hop", "jazz", "orquesta", "acustico", "acustica",
        "rock", "metal", "indie", "guaracha", "merengue", "crossover", "live set"
      );
      case "fiestas" -> List.of(
        "fiesta", "fiestas", "rumba", "rumbita", "parche", "farra", "after party", "party",
        "discoteca", "disco", "perreo", "guaro", "trago", "pool party", "baile",
        "rumbeadero", "after", "farrita", "tomadero", "parrandita", "parranda", "noche de rumba"
      );
      case "deporte" -> List.of(
        "deporte", "deportes", "futbol", "partido", "torneo", "cancha", "ejercicio",
        "microfutbol", "futsal", "baloncesto", "basket", "basketball",
        "voleibol", "voley", "running", "trote", "ciclismo", "ciclovia",
        "natacion", "tenis", "padel", "boxeo", "crossfit", "gimnasio",
        "gym", "senderismo", "trekking", "patinaje", "maraton", "maratón", "yoga", "spinning"
      );
      case "gastronomia" -> List.of(
        "gastronomia", "comida", "comer", "cena", "almuerzo", "brunch",
        "restaurante", "degustacion", "desayuno", "cafe", "cafeteria",
        "cata", "vino", "asado", "picada", "hamburguesa", "pizza",
        "coctel", "cocteles", "cocteleria", "tragos", "cerveza artesanal", "food truck", "tapas"
      );
      case "networking" -> List.of(
        "networking", "network", "negocios", "emprendedores", "emprendimiento",
        "contactos", "charla", "taller", "workshop", "conferencia",
        "congreso", "meetup", "feria", "seminario", "masterclass", "panel", "ponencia", "speakers"
      );
      case "arte" -> List.of(
        "arte", "artistico", "artistica", "galeria", "exposicion",
        "museo", "teatro", "cine", "pelicula", "stand up", "comedia",
        "poesia", "danza", "cultural", "fotografia", "pintura", "impro", "obra", "performance"
      );
      default -> List.of(normalized);
    };
  }

  private boolean matchesCity(Evento event, String city) {
    if (isBlank(city)) return true;
    return normalize(event.getCiudad()).contains(normalize(city));
  }

  private boolean matchesZone(Evento event, String zone) {
    if (zone == null || "__ANY__".equals(zone) || isBlank(zone)) return true;

    String haystack = normalize(
      safe(event.getDireccionCompleta()) + " " +
        safe(event.getUbicacion()) + " " +
        safe(event.getNombreLugar()) + " " +
        safe(event.getCiudad())
    );

    return haystack.contains(normalize(zone));
  }

  private boolean matchesHour(Evento event, LocalTime targetHour) {
    if (targetHour == null) return true;
    if (event.getHoraInicio() == null) return false;

    int diff = Math.abs(event.getHoraInicio().toSecondOfDay() - targetHour.toSecondOfDay());
    return diff <= (180 * 60);
  }

  private boolean matchesFreeOrPaid(Evento event, Boolean wantsFree) {
    if (wantsFree == null) return true;
    return Objects.equals(Boolean.TRUE.equals(event.getEventoGratuito()), wantsFree);
  }

  private boolean matchesBudget(Evento event, Integer budgetMax) {
    if (budgetMax == null) return true;
    if (Boolean.TRUE.equals(event.getEventoGratuito())) return true;
    if (event.getPrecio() == null) return false;
    return event.getPrecio().compareTo(BigDecimal.valueOf(budgetMax)) <= 0;
  }

  private String buildConversationKey(Principal principal, AssistantRequest request) {
    String owner;
    if (principal != null && !isBlank(principal.getName())) {
      owner = "user:" + principal.getName().trim().toLowerCase(Locale.ROOT);
    } else {
      String sessionId = request != null ? safe(request.sessionId()) : "";
      owner = !sessionId.isBlank() ? "guest:" + sessionId : "guest:default";
    }

    String conversationId = request != null ? safe(request.conversationId()) : "";
    if (!conversationId.isBlank()) {
      return owner + ":conversation:" + conversationId;
    }

    return owner;
  }

  private boolean isPlanIntent(String message) {
    return containsAny(message,
      "hazme un plan",
      "armame un plan",
      "organízame un plan",
      "organizame un plan",
      "recomiendame un plan",
      "recomiéndame un plan",
      "recomiendame algo",
      "recomiéndame algo",
      "ponme algo",
      "tirame algo",
      "tírame algo",
      "sugiereme algo",
      "sugiéreme algo",
      "dame un plan",
      "dame ideas",
      "algo para hacer",
      "que hay para hacer",
      "qué hay para hacer",
      "que se puede hacer",
      "qué se puede hacer",
      "quiero salir",
      "quiero un parche",
      "quiero un plan",
      "plan para hoy",
      "plan para esta noche",
      "sorprendeme con algo",
      "sorpréndeme con algo",
      "plancito",
      "plan tranquilo",
      "que me recomiendas hacer",
      "qué me recomiendas hacer",
      "que parche hay",
      "qué parche hay",
      "dame un parche",
      "botame un plan",
      "bótame un plan",
      "quiero hacer algo",
      "estoy desparchado",
      "estoy desparchada",
      "sin plan",
      "saqueme un plan",
      "sácame un plan"
    );
  }

  private boolean isPreferencesRecommendationIntent(String message) {
    return containsAny(message,
      "segun mis preferencias",
      "segun mis gustos",
      "basado en mis preferencias",
      "basado en mis gustos",
      "de acuerdo con mis gustos",
      "de acuerdo con mis preferencias",
      "segun lo que me gusta"
    );
  }

  private boolean isEventSearchIntent(String message) {
    return containsAny(message,
      "quiero un evento",
      "busco un evento",
      "eventos de",
      "eventos en",
      "hay eventos en",
      "hay un evento de",
      "hay algun evento de",
      "hay algún evento de",
      "hay un concierto de",
      "buscame eventos",
      "búscame eventos",
      "muestrame eventos",
      "muéstrame eventos",
      "mostrame eventos",
      "ver eventos",
      "que eventos hay",
      "qué eventos hay",
      "que hay para hacer",
      "qué hay para hacer",
      "que hay en",
      "qué hay en",
      "hay algo en",
      "algo para hacer",
      "algun evento",
      "algún evento",
      "algun concierto",
      "algún concierto",
      "recomiendame algo",
      "recomiéndame algo",
      "quiero hacer algo",
      "actividad",
      "actividades",
      "salida",
      "vuelta",
      "parche",
      "parches",
      "fiesta",
      "futbol",
      "musica",
      "gastronomia",
      "gaming",
      "arte",
      "networking",
      "deporte",
      "deportes",
      "karaoke",
      "salsa",
      "bachata",
      "reggaeton",
      "techno",
      "house",
      "stand up",
      "comedia",
      "teatro",
      "cine",
      "brunch",
      "restaurante",
      "taller",
      "workshop",
      "charla",
      "meetup",
      "rumba",
      "feria",
      "festival",
      "show",
      "toque",
      "planes en",
      "que parche hay",
      "qué parche hay",
      "que vuelta hay",
      "qué vuelta hay"
    );
  }

  private boolean isAboutParchaFaceQuestion(String message) {
    String normalized = normalize(message);

    return normalized.contains("parchaface") && containsAny(normalized,
      "que es",
      "quien es",
      "para que sirve",
      "de que trata",
      "que hace",
      "como funciona"
    );
  }

  private boolean isHowToIntent(String message) {
    return containsAny(message,
      "paso a paso",
      "dame un paso a paso",
      "como hago",
      "cómo hago",
      "como puedo",
      "cómo puedo",
      "como se",
      "cómo se",
      "ayudame a",
      "ayúdame a",
      "explicame como",
      "explícame cómo",
      "guiame para",
      "guíame para",
      "orientame",
      "oriéntame",
      "como hago pa",
      "cómo hago pa"
    );
  }

  private boolean isGoToFeatureIntent(String message) {
    return containsAny(message,
      "llevame",
      "llévame",
      "llevarme",
      "guiame",
      "guíame",
      "dirigeme",
      "dirígeme",
      "abre",
      "abrime",
      "ábrime",
      "abreme",
      "ábreme",
      "quiero ir",
      "quiero entrar",
      "ir al",
      "ir a",
      "ve al",
      "ve a",
      "vamos al",
      "vamos a",
      "mandame",
      "mándame",
      "pasame",
      "pásame",
      "tirame pa",
      "tírame pa",
      "llevame pa",
      "llévame pa"
    );
  }

  private boolean isExploreNavigationIntent(String message) {
    return isGoToFeatureIntent(message)
      && containsAny(message, "explore", "explorar", "ver eventos", "eventos", "listado de eventos", "inicio de eventos")
      && !containsAny(message, "crear evento", "crear un evento", "publicar evento", "publicar un evento",
      "inicio", "pagina principal", "página principal");
  }

  private boolean isRandomNearbyEventIntent(String message) {
    return containsAny(message,
      "evento al azar",
      "evento aleatorio",
      "evento random",
      "evento ramdom",
      "evento cualquiera",
      "llevame a un evento al azar",
      "llévame a un evento al azar",
      "llevame a un evento aleatorio",
      "llévame a un evento aleatorio",
      "llevame a un evento random",
      "llévame a un evento random",
      "llevame a un evento ramdom",
      "llévame a un evento ramdom",
      "un evento en mi ciudad al azar",
      "un evento cerca de mi al azar",
      "un evento cerca de mí al azar",
      "sorprendeme con un evento",
      "sorpréndeme con un evento",
      "sorprendeme",
      "sorpréndeme");
  }

  private boolean isAvailableEventsQuestion(String message) {
    return containsAny(message,
      "que eventos hay",
      "qué eventos hay",
      "que eventos hay disponibles",
      "qué eventos hay disponibles",
      "mostrar eventos",
      "muestrame eventos",
      "muéstrame eventos",
      "listar eventos",
      "lista de eventos",
      "eventos disponibles",
      "hay eventos en",
      "si hay eventos en",
      "que hay en",
      "qué hay en",
      "hay algo en",
      "que planes hay",
      "qué planes hay",
      "que hay para hacer",
      "qué hay para hacer",
      "que actividades hay",
      "qué actividades hay",
      "mostrame que hay",
      "que parche hay",
      "qué parche hay",
      "que vuelta hay",
      "qué vuelta hay",
      "que hay pa hacer",
      "qué hay pa hacer",
      "hay algo pa hoy",
      "qué se mueve"
    );
  }

  private boolean isPaymentMethodsQuestion(String message) {
    return containsAny(message,
      "metodos de pago",
      "métodos de pago",
      "como puedo pagar",
      "cómo puedo pagar",
      "con que puedo pagar",
      "con qué puedo pagar",
      "formas de pago",
      "como se paga",
      "cómo se paga",
      "como hago el pago",
      "cómo hago el pago",
      "reciben tarjeta",
      "aceptan tarjeta",
      "aceptan nequi",
      "se puede pagar por pse",
      "medios de pago",
      "formas para pagar",
      "con que medios pago",
      "con qué medios pago",
      "puedo pagar con nequi",
      "puedo pagar con tarjeta",
      "aceptan pse",
      "aceptan debito",
      "aceptan débito",
      "aceptan credito",
      "aceptan crédito"
    );
  }

  private boolean isTermsQuestion(String message) {
    return containsAny(message,
      "terminos y condiciones",
      "términos y condiciones",
      "terminos",
      "términos",
      "reglas de uso",
      "condiciones de uso",
      "acuerdo de uso",
      "condiciones del servicio"
    );
  }

  private boolean isConductRulesQuestion(String message) {
    String normalized = normalize(message);

    boolean asksForRules = containsAny(normalized,
      "reglas",
      "normas",
      "esta permitido",
      "no esta permitido",
      "se puede",
      "puedo decir",
      "puedo escribir",
      "comportamiento",
      "convivencia"
    );

    boolean topic = containsAny(normalized,
      "grosero",
      "groserias",
      "insultar",
      "insultos",
      "ofensivo",
      "ofensiva",
      "acoso",
      "amenazas",
      "spam",
      "lenguaje inapropiado",
      "discriminacion",
      "fraude"
    );

    return asksForRules && topic;
  }

  private String buildConductRulesReply() {
    return """
                Sí. En ParchaFace no deberías publicar ni enviar contenido ofensivo, insultos, amenazas, acoso, discriminación, fraude, spam o información engañosa.
                Tampoco deberías usar lenguaje grosero para atacar a otras personas, humillar, hostigar, intimidar, publicar eventos falsos o compartir contenido ilegal.
                Si alguien incumple estas reglas, el contenido puede ser reportado, removido y la cuenta puede ser suspendida o bloqueada por administración.
                """.trim();
  }

  private boolean isPrivacyQuestion(String message) {
    return containsAny(message,
      "politica de privacidad",
      "política de privacidad",
      "privacidad",
      "datos personales",
      "tratamiento de datos",
      "mis datos",
      "proteccion de datos",
      "protección de datos"
    );
  }

  private boolean isTransportQuestion(String message) {
    return containsAny(message,
      "transporte",
      "movilidad",
      "taxi",
      "uber",
      "didi",
      "indrive",
      "picap",
      "como me voy",
      "cómo me voy",
      "como llego",
      "cómo llego",
      "como me transporto",
      "cómo me transporto",
      "como me muevo",
      "cómo me muevo",
      "como regreso",
      "cómo regreso"
    );
  }

  private boolean isEmergencyQuestion(String message) {
    return containsAny(message,
      "emergencia",
      "emergencias",
      "numero de emergencia",
      "número de emergencia",
      "bomberos",
      "ambulancia",
      "policia",
      "policía",
      "linea 123",
      "línea 123",
      "urgencias",
      "llamar a emergencias",
      "numero de bomberos",
      "número de bomberos"
    );
  }

  private boolean isRouteQuestion(String message) {
    return containsAny(message,
      "donde queda",
      "dónde queda",
      "donde esta",
      "dónde está",
      "como llego",
      "cómo llego",
      "donde es la seccion",
      "dónde es la sección",
      "donde estan los comentarios",
      "dónde están los comentarios",
      "como entro",
      "cómo entro",
      "en donde queda",
      "en dónde queda",
      "por donde entro",
      "por dónde entro"
    );
  }

  private boolean seemsRelevantToParchaFace(String message) {
    String normalized = normalize(message);
    return containsAny(message,
      "parchaface",
      "evento",
      "eventos",
      "fiesta",
      "plan",
      "community",
      "comunidad",
      "perfil",
      "preferencias",
      "gustos",
      "mapa",
      "comentario",
      "discusion",
      "discusión",
      "crear evento",
      "inscrib",
      "pago",
      "pagar",
      "entrada",
      "boleta",
      "boleto",
      "ticket",
      "privacidad",
      "terminos",
      "términos",
      "transporte",
      "emergencia",
      "inicio",
      "pagina principal",
      "página principal",
      "home",
      "hola",
      "gracias",
      "chao",
      "jaja",
      "parche",
      "rumba",
      "show",
      "concierto"
    ) || detectCategory(normalized) != null || !detectCities(message).isEmpty();
  }

  private boolean isDirectManipulationRequest(String message) {
    String normalized = normalize(message);

    return normalized.contains("creame un evento")
      || normalized.contains("crea un evento por mi")
      || normalized.contains("hazme un evento")
      || normalized.contains("editalo por mi")
      || normalized.contains("editalo por mi")
      || normalized.contains("edita el evento por mi")
      || normalized.contains("publicalo por mi")
      || normalized.contains("sube el evento por mi")
      || normalized.contains("monta el evento por mi")
      || normalized.contains("armame el evento")
      || normalized.contains("hazlo por mi")
      || normalized.contains("rellename eso por mi");
  }

  private boolean containsProfanity(String message) {
    String normalized = normalize(message)
      .replace("0", "o")
      .replace("@", "a")
      .replace("1", "i")
      .replace("3", "e")
      .replace("4", "a")
      .replace("5", "s")
      .replace("7", "t")
      .replace("$", "s")
      .replaceAll("[^a-z0-9ñ\\s]", " ")
      .replaceAll("\\s+", " ")
      .trim();

    return Pattern.compile(
      "\\b(?:hp|hpta|hptas|hijueputa|hijueputas|hijo de puta|hija de puta|hijodeputa|jueputa|jueputa|jueputas|puta|puto|putita|putito|putazo|gonorrea|gonorreas|gono|carechimba|care monda|caremonda|care mond[aá]|careverga|careculo|malparido|malparida|malparidos|marica|marico|maricon|maricona|mariquita|pendejo|pendeja|pendejada|cabron|cabrona|huevon|huevona|wevon|webon|guevon|guevona|culero|culera|culiado|culiada|mierda|mierdero|imbecil|imbeciles|idiota|idiotas|estupido|estupida|tarado|tarada|baboso|babosa|mamaguevo|mamahuevo|comemierda|pirobo|piroba|zorra|perra|cono|coño|carajo|chingar|chingada|chingado|pinche|pelotudo|pelotuda|boludo|boluda|verga|vergas|mk|mka|gonorreita|lampara|lámpara|sapo hijueputa|triplehijueputa|hpta madre|hp madre|fuck|fucking|fucker|motherfucker|mother fucker|shit|bullshit|asshole|ass hole|bitch|son of a bitch|bastard|slut|whore|dick|cock|pussy|cunt|retard|dumbass|jackass|piece of shit)\\b"
    ).matcher(normalized).find();
  }

  private String detectCategory(String message) {
    String normalized = normalize(message);

    int deporte = scoreVocabulary(normalized,
      "futbol", "deporte", "deportes", "deportivo", "deportiva", "partido", "cancha",
      "ejercicio", "competencia", "torneo", "microfutbol", "futsal", "baloncesto",
      "basket", "basketball", "voleibol", "voley", "running", "trote", "ciclismo",
      "ciclovia", "natacion", "tenis", "padel", "boxeo", "crossfit", "gym",
      "gimnasio", "senderismo", "trekking", "patinaje", "maraton", "maratón", "yoga", "spinning"
    );

    int musica = scoreVocabulary(normalized,
      "musica", "concierto", "conciertos", "show", "toque", "presentacion", "presentación",
      "dj", "banda", "festival", "karaoke", "salsa", "bachata", "vallenato",
      "reggaeton", "electronica", "techno", "house", "rap", "hip hop", "jazz",
      "orquesta", "cantante", "acustico", "acustica", "rock", "metal", "indie", "guaracha", "merengue"
    );

    int arte = scoreVocabulary(normalized,
      "arte", "artistico", "artistica", "exposicion", "galeria", "museo", "teatro",
      "cine", "pelicula", "stand up", "comedia", "poesia", "danza", "cultural",
      "fotografia", "pintura", "impro", "obra", "performance"
    );

    int gastronomia = scoreVocabulary(normalized,
      "gastronomia", "comida", "comer", "restaurante", "cenar", "almorzar", "brunch",
      "desayuno", "cafe", "cafeteria", "degustacion", "cata", "vino", "asado",
      "picada", "hamburguesa", "pizza", "coctel", "cocteles", "cocteleria", "tragos", "food truck", "tapas"
    );

    int networking = scoreVocabulary(normalized,
      "networking", "network", "emprendedores", "contactos", "negocios", "emprendimiento",
      "charla", "taller", "workshop", "conferencia", "congreso", "meetup",
      "feria", "seminario", "masterclass", "panel", "ponencia", "speakers"
    );

    int gaming = scoreVocabulary(normalized,
      "gaming", "gamer", "videojuegos", "videojuego", "juegos", "esports", "e sports", "e-sports",
      "lan party", "consola", "playstation", "ps5", "ps4", "xbox", "nintendo", "switch", "steam",
      "fortnite", "fifa", "fc 25", "fc25", "valorant", "lol", "league of legends", "dota", "free fire", "minecraft", "cod", "warzone"
    );

    int fiestas = scoreVocabulary(normalized,
      "fiesta", "fiestas", "rumba", "rumbita", "parche", "farra", "party",
      "after party", "discoteca", "disco", "perreo", "guaro", "trago",
      "pool party", "electro", "electronica", "techno", "house", "baile",
      "rumbeadero", "after", "farrita", "parranda", "tomadero"
    );

    int best = Math.max(
      Math.max(Math.max(deporte, musica), Math.max(arte, gastronomia)),
      Math.max(networking, Math.max(gaming, fiestas))
    );

    if (best == 0) {
      return null;
    }

    if (best == deporte) return "DEPORTE";
    if (best == musica) return "MUSICA";
    if (best == arte) return "ARTE";
    if (best == gastronomia) return "GASTRONOMIA";
    if (best == networking) return "NETWORKING";
    if (best == gaming) return "GAMING";
    return "FIESTAS";
  }

  private String detectCity(String message) {
    List<String> cities = detectCities(message);
    return cities.isEmpty() ? null : cities.get(0);
  }

  private List<String> detectCities(String message) {
    if (isBlank(message)) {
      return List.of();
    }

    String normalizedMessage = normalize(message);
    List<String> knownCities = eventoService.getEventosPublicos().stream()
      .map(Evento::getCiudad)
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(city -> !city.isBlank())
      .distinct()
      .toList();

    LinkedHashSet<String> matches = new LinkedHashSet<>();

    for (String city : knownCities) {
      if (normalizedMessage.contains(normalize(city))) {
        matches.add(city);
      }
    }

    if (!matches.isEmpty()) {
      return matches.stream().toList();
    }

    String directBest = findBestMatchingCity(message, knownCities);
    if (!isBlank(directBest)) {
      matches.add(directBest);
      return matches.stream().toList();
    }

    Matcher matcher = Pattern.compile("(?i)\\b(?:en|por|desde|cerca de|cerca a|a)\\s+([a-zA-Záéíóúñ\\s]{3,40})").matcher(message);
    while (matcher.find()) {
      String raw = matcher.group(1)
        .replaceAll("(?i)\\b(gratis|de pago|pago|pagado|a las|con|sin|para|porfa|por favor|y|pero|si|sí|si es|quiero|necesito|evento|eventos|azar|aleatorio|random|ramdom)\\b.*$", "")
        .trim();

      String bestAfterPrefix = findBestMatchingCity(raw, knownCities);
      if (!isBlank(bestAfterPrefix)) {
        matches.add(bestAfterPrefix);
      } else if (!raw.isBlank()) {
        matches.add(toTitleCase(raw));
      }
    }

    return matches.stream().toList();
  }

  private String findBestMatchingCity(String rawText, List<String> knownCities) {
    String normalizedRaw = normalize(rawText);
    if (normalizedRaw.isBlank() || knownCities.isEmpty()) {
      return null;
    }

    String[] words = normalizedRaw.split("[^a-z0-9ñ]+");
    String bestCity = null;
    int bestDistance = Integer.MAX_VALUE;

    for (String city : knownCities) {
      String normalizedCity = normalize(city);

      if (normalizedRaw.equals(normalizedCity)) {
        return city;
      }

      for (String word : words) {
        if (word.isBlank()) continue;

        int distance = editDistance(word, normalizedCity);
        int allowed = normalizedCity.length() >= 8 ? 2 : 1;

        if (distance <= allowed && distance < bestDistance) {
          bestDistance = distance;
          bestCity = city;
        }
      }
    }

    return bestCity;
  }

  private String detectZone(String message) {
    String normalized = normalize(message);

    if (containsAny(normalized, "zona norte", "norte", "al norte", "por el norte", "norte de la ciudad")) return "norte";
    if (containsAny(normalized, "zona sur", "sur", "al sur", "por el sur", "sur de la ciudad")) return "sur";
    if (containsAny(normalized, "zona centro", "centro", "centro de la ciudad", "por el centro")) return "centro";
    if (containsAny(normalized, "oriente", "zona oriental", "al oriente", "por el oriente")) return "oriente";
    if (containsAny(normalized, "occidente", "zona occidental", "al occidente", "por el occidente")) return "occidente";

    return null;
  }

  private LocalTime detectHour(String message) {
    Matcher matcher12 = TWELVE_HOUR_PATTERN.matcher(message);
    if (matcher12.find()) {
      int hour = Integer.parseInt(matcher12.group(1));
      int minute = matcher12.group(2) != null ? Integer.parseInt(matcher12.group(2)) : 0;
      String meridiem = matcher12.group(3).toLowerCase(Locale.ROOT);

      if ("pm".equals(meridiem) && hour < 12) hour += 12;
      if ("am".equals(meridiem) && hour == 12) hour = 0;

      return LocalTime.of(hour, minute);
    }

    Matcher matcher24 = TWENTY_FOUR_HOUR_PATTERN.matcher(message);
    if (matcher24.find()) {
      int hour = Integer.parseInt(matcher24.group(1));
      int minute = Integer.parseInt(matcher24.group(2));
      if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
        return LocalTime.of(hour, minute);
      }
    }

    return null;
  }

  private Integer detectBudget(String message) {
    String normalized = normalize(message);

    if (containsAny(normalized, "barato", "baratico", "economico", "económico", "bajo", "poquita plata", "sin mucha plata", "presupuesto corto", "comodo", "cómodo")) return 20000;
    if (containsAny(normalized, "medio", "mas o menos", "más o menos", "intermedio", "normal")) return 50000;
    if (containsAny(normalized, "alto", "caro", "carito", "premium", "sin limite", "sin límite")) return 100000;

    Matcher matcher = BUDGET_PATTERN.matcher(message);
    if (!matcher.find()) return null;

    String raw = matcher.group(1)
      .toLowerCase(Locale.ROOT)
      .replace("$", "")
      .replace(" ", "")
      .trim();

    boolean hasK = raw.endsWith("k");

    raw = raw.replace("k", "")
      .replace(".", "")
      .replace(",", "");

    try {
      int value = Integer.parseInt(raw);
      return hasK ? value * 1000 : value;
    } catch (Exception e) {
      return null;
    }
  }

  private Boolean detectYesNo(String normalized) {
    if (containsAny(normalized, "si", "sí", "claro", "de una", "quiero", "dale", "sisas", "obvio", "hagale", "hágale", "de one", "me sirve", "copiado", "ok", "listo")) {
      return true;
    }

    if (containsAny(normalized, "no", "sin eso", "no quiero", "no necesito", "nel", "nelson", "nope", "negativo", "paso", "mejor no")) {
      return false;
    }

    return null;
  }

  private Boolean detectTransportPreference(String message) {
    String normalized = normalize(message);

    if (containsAny(normalized,
      "sin transporte", "no transporte", "no quiero transporte", "no necesito transporte",
      "sin taxi", "sin uber", "sin didi", "sin indrive", "sin picap", "yo me voy por mi cuenta", "yo llego solo", "yo llego sola")) {
      return false;
    }

    if (containsAny(normalized,
      "con transporte", "incluye transporte", "quiero transporte", "necesito transporte",
      "con taxi", "con uber", "con didi", "con indrive", "con picap", "ayudame con el transporte", "ayúdame con el transporte", "quiero saber como llegar", "quiero saber cómo llegar")) {
      return true;
    }

    if (containsAny(normalized, "transporte", "taxi", "uber", "didi", "indrive", "picap", "movilidad", "como llego", "cómo llego")) {
      return true;
    }

    return null;
  }

  private Boolean detectFoodPreference(String message) {
    String normalized = normalize(message);

    if (containsAny(normalized,
      "sin comida", "no comida", "no quiero comida", "no quiero restaurantes", "sin restaurantes",
      "sin comer", "sin cena", "sin almuerzo", "sin brunch", "no quiero ir a comer", "sin traguito", "sin tragos")) {
      return false;
    }

    if (containsAny(normalized,
      "con comida", "quiero comida", "quiero restaurantes", "quiero comer",
      "con restaurantes", "con cena", "con almuerzo", "con brunch", "con tragos", "con cocteles", "con cócteles", "quiero ir a comer algo")) {
      return true;
    }

    if (containsAny(normalized, "comer", "restaurante", "comida", "cenar", "almorzar", "brunch", "desayunar", "tragos", "cocteles", "cócteles", "cafecito")) {
      return true;
    }

    return null;
  }

  private AssistantResponse replyWithNavigate(String reply, String route) {
    return new AssistantResponse(
      reply,
      List.of(new AssistantActionDto("navigate", route, null, null, null)),
      List.of()
    );
  }

  private AssistantResponse simpleReply(String reply) {
    return new AssistantResponse(reply, List.of(), List.of());
  }

  private boolean containsAny(String source, String... candidates) {
    String normalizedSource = normalize(source);
    Set<String> sourceTokens = comparableTokenSet(normalizedSource);

    for (String candidate : candidates) {
      String normalizedCandidate = normalize(candidate);
      if (normalizedCandidate.isBlank()) {
        continue;
      }

      if (normalizedSource.contains(normalizedCandidate)) {
        return true;
      }

      Set<String> candidateTokens = comparableTokenSet(normalizedCandidate);

      if (!candidateTokens.isEmpty() && sourceTokens.containsAll(candidateTokens)) {
        return true;
      }

      if (candidateTokens.size() > 1 && overlapRatio(sourceTokens, candidateTokens) >= 0.80d) {
        return true;
      }

      for (String token : candidateTokens) {
        if (token.length() >= 4 && fuzzyTokenMatch(normalizedSource, token)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean fuzzyTokenMatch(String source, String candidate) {
    String[] tokens = source.split("[^a-z0-9ñ]+");
    String comparableCandidate = toComparableToken(candidate);
    int maxDistance = comparableCandidate.length() >= 8 ? 2 : 1;

    for (String token : tokens) {
      String comparableToken = toComparableToken(token);
      if (comparableToken.isBlank() || comparableToken.length() < 4) continue;

      if (editDistance(comparableToken, comparableCandidate) <= maxDistance) {
        return true;
      }
    }

    return false;
  }

  private int editDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];

    for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
    for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

        dp[i][j] = Math.min(
          Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
          dp[i - 1][j - 1] + cost
        );
      }
    }

    return dp[a.length()][b.length()];
  }

  private String normalize(String value) {
    if (value == null) return "";

    String normalized = value
      .toLowerCase(Locale.ROOT)
      .replace("á", "a")
      .replace("é", "e")
      .replace("í", "i")
      .replace("ó", "o")
      .replace("ú", "u")
      .replace("ü", "u")
      .replaceAll("\\bq\\b", "que")
      .replaceAll("\\bk\\b", "que")
      .replaceAll("\\bxq\\b", "porque")
      .replaceAll("\\bpa\\b", "para")
      .replaceAll("\\bpal\\b", "para el")
      .replaceAll("\\bparce\\b", "amigo")
      .replaceAll("\\bparcero\\b", "amigo")
      .replaceAll("\\bbro\\b", "amigo")
      .replaceAll("\\bmano\\b", "amigo")
      .replaceAll("\\bqlq\\b", "que lo que")
      .replaceAll("\\bklk\\b", "que lo que")
      .replaceAll("[^a-z0-9ñ\\s]", " ")
      .replaceAll("\\s+", " ")
      .trim();

    normalized = canonicalizeSemanticText(normalized)
      .replaceAll("\\s+", " ")
      .trim();

    return normalized;
  }

  private String canonicalizeSemanticText(String value) {
    String normalized = value;

    normalized = normalized
      .replaceAll("\\b(planecito|planazo|plancito|salidita|salidota|salida|parchecito|parchazo)\\b", "plan")
      .replaceAll("\\b(vaina|vueltica|vuelta|actividad|actividades|cosita|planecito)\\b", "evento")
      .replaceAll("\\b(sugiereme|sugierime|sugiere|proponme|propon|tirame|lanzame|ponme|botame|tirame algo)\\b", "recomiendame")
      .replaceAll("\\b(dirigeme|guiame|mandame|pasame|llevame pa)\\b", "llevame")
      .replaceAll("\\b(meterme|ingresar|loguearme|logearme)\\b", "entrar")
      .replaceAll("\\b(encuentrame|encontrame|consigueme|traeme)\\b", "buscame")
      .replaceAll("\\b(muestrame|mostrame|ensename|enseñame)\\b", "muestrame")
      .replaceAll("\\b(rumbear|farrear|perrear|rumbiar)\\b", "fiesta")
      .replaceAll("\\b(parchado|parchadito|tranqui|relajado|suavecito|relax)\\b", "plan tranquilo")
      .replaceAll("\\b(musiquita|musicita|rolita)\\b", "musica")
      .replaceAll("\\b(comelona|comidita|mecato)\\b", "comida")
      .replaceAll("\\b(gym)\\b", "gimnasio")
      .replaceAll("\\b(medallo)\\b", "medellin")
      .replaceAll("\\b(boleta|boletas|boleto|boletos|ticket|tickets|entrada|entradas|cover)\\b", "entrada")
      .replaceAll("\\b(plata|lucas)\\b", "presupuesto");

    return normalized;
  }

  private Set<String> comparableTokenSet(String text) {
    LinkedHashSet<String> tokens = new LinkedHashSet<>();
    for (String rawToken : normalize(text).split("\\s+")) {
      String token = toComparableToken(rawToken);
      if (token.length() >= 3) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  private String toComparableToken(String token) {
    String normalized = normalize(token);

    if (normalized.endsWith("es") && normalized.length() > 4) {
      normalized = normalized.substring(0, normalized.length() - 2);
    } else if (normalized.endsWith("s") && normalized.length() > 4) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    if (normalized.endsWith("cito") || normalized.endsWith("cita")) {
      normalized = normalized.substring(0, normalized.length() - 4);
    }

    return normalized.trim();
  }

  private double overlapRatio(Set<String> sourceTokens, Set<String> candidateTokens) {
    if (candidateTokens.isEmpty()) {
      return 0;
    }

    long matched = candidateTokens.stream().filter(sourceTokens::contains).count();
    return (double) matched / (double) candidateTokens.size();
  }

  private int scoreVocabulary(String normalizedMessage, String... vocabulary) {
    int score = 0;
    Set<String> seen = new HashSet<>();

    for (String term : vocabulary) {
      String normalizedTerm = normalize(term);
      if (normalizedTerm.isBlank() || seen.contains(normalizedTerm)) {
        continue;
      }

      if (containsAny(normalizedMessage, term)) {
        score += normalizedTerm.contains(" ") ? 3 : 2;
        seen.add(normalizedTerm);
      }
    }

    return score;
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isBlank();
  }

  private Double toDouble(Object value) {
    if (value == null) return null;

    if (value instanceof Number number) {
      return number.doubleValue();
    }

    try {
      return Double.parseDouble(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0;

    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
      + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
      * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private String toTitleCase(String text) {
    if (isBlank(text)) return text;

    String[] words = text.trim().split("\\s+");
    StringBuilder sb = new StringBuilder();

    for (String word : words) {
      if (word.isBlank()) continue;
      if (!sb.isEmpty()) sb.append(" ");
      sb.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
      if (word.length() > 1) {
        sb.append(word.substring(1).toLowerCase(Locale.ROOT));
      }
    }

    return sb.toString();
  }

  private enum GuidedMode {
    NONE, SEARCH, PLAN
  }

  private enum DiscoveryContext {
    NONE, AVAILABILITY, SEARCH, PREFERENCES, PLAN
  }

  private enum Awaiting {
    NONE,
    CATEGORY,
    FREE_OR_PAID,
    BUDGET,
    CITY,
    TRANSPORT,
    FOOD
  }

  private static final class ConversationMemory {
    private GuidedMode mode = GuidedMode.NONE;
    private Awaiting awaiting = Awaiting.NONE;

    private String category;
    private String city;
    private String zone;
    private Boolean wantsFree;
    private Integer budgetMax;
    private LocalTime targetHour;
    private Boolean needsTransport;
    private Boolean wantsFoodSuggestions;
    private boolean usePreferenceCategories;
    private Double referenceLat;
    private Double referenceLng;
    private String intentQuery;
    private String lastTopicQuery;
    private String mood;
    private DiscoveryContext lastDiscoveryContext = DiscoveryContext.NONE;

    private Integer currentEventId;
    private String currentEventTitle;
    private String currentEventCity;
    private String currentEventCategory;
    private Integer currentEventPrice;
    private Boolean currentEventIsFree;

    private List<Integer> lastShownEventIds = new ArrayList<>();
    private List<String> lastShownEventTitles = new ArrayList<>();
    private Integer lastFocusedEventId;

    private String lastRoute;
    private String lastUserMessage;
    private String lastAssistantMessage;

    private long updatedAt;

    private void touch() {
      this.updatedAt = System.currentTimeMillis();
    }

    private void resetGuided() {
      this.mode = GuidedMode.NONE;
      this.awaiting = Awaiting.NONE;
      this.category = null;
      this.city = null;
      this.zone = null;
      this.wantsFree = null;
      this.budgetMax = null;
      this.targetHour = null;
      this.needsTransport = null;
      this.wantsFoodSuggestions = null;
      this.usePreferenceCategories = false;
      this.intentQuery = null;
      this.mood = null;
      this.updatedAt = System.currentTimeMillis();
    }
  }

  private boolean containsHowToCue(String message) {
    return containsAny(message,
      "paso a paso",
      "como",
      "cómo",
      "ayudame",
      "ayúdame",
      "guia",
      "guía",
      "instrucciones",
      "explicame",
      "explícame",
      "orientame",
      "oriéntame",
      "como hago pa",
      "cómo hago pa",
      "como funciona",
      "cómo funciona"
    );
  }
}
