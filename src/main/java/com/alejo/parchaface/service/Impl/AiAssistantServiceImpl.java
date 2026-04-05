package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.AssistantActionDto;
import com.alejo.parchaface.dto.AssistantOptionDto;
import com.alejo.parchaface.dto.AssistantRequest;
import com.alejo.parchaface.dto.AssistantResponse;
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
            if (containsAny(normalizedMessage, "comentarios", "comentario") && currentRoute.startsWith("/event/")) {
                return new AssistantResponse(
                        "Te explico y además te llevo: en el detalle del evento baja un poco y encontrarás la sección de comentarios. Te muevo hasta allí.",
                        List.of(new AssistantActionDto("scroll", null, "comments-section", null, null)),
                        List.of()
                );
            }

            if (containsAny(normalizedMessage, "community", "comunidad", "discusion", "discusión")) {
                return new AssistantResponse(
                        "La sección community es el espacio donde puedes ver publicaciones, discusiones e interactuar con la comunidad. Te llevo ahora.",
                        List.of(new AssistantActionDto("navigate", ROUTE_COMMUNITY, null, null, null)),
                        List.of()
                );
            }

            if (containsAny(normalizedMessage, "perfil", "mi perfil")) {
                return new AssistantResponse(
                        "En tu perfil puedes ver tu actividad, tus eventos creados y tus eventos inscritos. Te llevo ahora.",
                        List.of(new AssistantActionDto("navigate", ROUTE_PROFILE, null, null, null)),
                        List.of()
                );
            }

            if (containsAny(normalizedMessage, "mapa")) {
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

        if (trimmed.matches("^(hola|holi|buenas|hello|hey|ey|oe|epa|aja|aja|qlq|klk|que mas|que hubo|quiubo|kiubo)$")) {
            return simpleReply("Hola 👋 ¿Qué quieres hacer en ParchaFace? Puedo recomendarte eventos, armarte un plan o llevarte al mapa de eventos.");
        }

        if (trimmed.matches("^(buenos dias|buen dia|buenas tardes|buenas noches|wenas)$")) {
            return simpleReply("Hola 👋 Bienvenido a ParchaFace. Dime qué te gustaría hacer y te ayudo.");
        }

        if (containsAny(normalized,
                "oe y entonces", "oe entonces", "y entonces", "entonces que",
                "aja y entonces", "que mas pues", "que hubo pues")) {
            return simpleReply("Aquí estoy 😄 Dime qué quieres hacer y te ayudo. Por ejemplo: buscar eventos, armarte un plan o llevarte al mapa.");
        }

        if (trimmed.matches("^(mucho gusto|un gusto|encantado|encantada)$")) {
            return simpleReply("Mucho gusto 😄 Yo soy tu asistente de ParchaFace. Cuando quieras te ayudo con eventos, planes o rutas dentro de la app.");
        }

        if (trimmed.matches("^(gracias|muchas gracias|mil gracias|thanks|graciaas)$")) {
            return simpleReply("Con gusto 😄 Si quieres, sigo ayudándote con eventos, planes o rutas dentro de la app.");
        }

        if (trimmed.matches("^(chao|adios|nos vemos|hasta luego|bye|hablamos)$")) {
            return simpleReply("Chao 👋 Que te vaya súper. Cuando quieras, vuelves y te ayudo con otro plan o evento.");
        }

        if (trimmed.matches("^(como estas|que tal|todo bien|todo bn|todo bien o que)$")) {
            return simpleReply("Todo bien 😄 Listo para ayudarte con ParchaFace. ¿Qué necesitas?");
        }

        if (trimmed.matches("^(jaja+|jajaja+|jeje+|jiji+|xd+|lol+)$")) {
            return simpleReply("Jajaja 😄 no sé de qué te ríes, pero acá sí te puedo encontrar eventos y planes que te hagan pasar bueno.");
        }

        if (trimmed.matches("^(ok|okay|okey|vale|listo|de una|dale|perfecto|entiendo|hagale)$")) {
            return simpleReply("De una 🙌 dime qué necesitas y seguimos.");
        }

        if (trimmed.matches("^(perdon|disculpa|disculpame)$")) {
            return simpleReply("Tranqui 😄 seguimos. Dime qué necesitas en ParchaFace y te ayudo.");
        }

        return null;
    }

    private AssistantResponse buildHowToResponse(String message, String currentRoute) {
        if (!containsHowToCue(message)) {
            return null;
        }

        if ((containsAny(message, "evento") && containsAny(message, "crear", "publicar", "hacer"))
                || containsAny(message,
                "crear evento", "crear un evento", "publicar evento", "publicar un evento",
                "hacer evento", "hacer un evento", "creo un evento", "como creo un evento",
                "cómo creo un evento", "como hago un evento", "cómo hago un evento")) {
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
                "cambiar perfil", "actualizar perfil", "modificar perfil", "cambiar mi foto", "cambiar mis datos")) {
            return simpleReply("""
                Paso a paso para editar tu perfil en ParchaFace:

                1. Ve a tu perfil.
                2. Busca la opción de editar perfil o la parte de datos personales.
                3. Cambia la información que necesites, como nombre, foto o datos visibles.
                4. Guarda los cambios.
                5. Revisa que la información haya quedado actualizada correctamente.
                """.trim());
        }

        if (containsAny(message, "inicio", "pagina principal", "página principal", "home")) {
            return simpleReply("""
                Paso a paso para usar la página principal de ParchaFace:

                1. Entra al inicio.
                2. Revisa los eventos destacados o disponibles.
                3. Usa los filtros si quieres buscar algo más específico.
                4. Entra al detalle del evento que te interese.
                5. Desde allí puedes ver información, ubicación, precio y acciones disponibles.
                """.trim());
        }

        if (containsAny(message, "explorar", "buscar eventos", "ver eventos", "eventos disponibles")) {
            return simpleReply("""
                Paso a paso para explorar eventos en ParchaFace:

                1. Entra a la sección de explorar eventos.
                2. Revisa la lista de eventos disponibles.
                3. Usa filtros por categoría, ciudad, precio o lo que necesites.
                4. Abre el detalle del evento que te guste.
                5. Desde allí puedes decidir si quieres verlo mejor, comentarlo o inscribirte.
                """.trim());
        }

        if (containsAny(message, "mapa")) {
            return simpleReply("""
                Paso a paso para usar el mapa en ParchaFace:

                1. Entra a la sección del mapa.
                2. Mira los eventos ubicados cerca de ti o en otra zona.
                3. Pulsa el marcador del evento que te interese.
                4. Revisa el resumen o entra al detalle completo.
                5. Desde allí puedes comparar ubicaciones y decidir cuál te conviene más.
                """.trim());
        }

        if (containsAny(message, "community", "comunidad", "discusiones", "discusion", "discusión")) {
            return simpleReply("""
                Paso a paso para usar community en ParchaFace:

                1. Entra a la sección community.
                2. Revisa publicaciones o discusiones activas.
                3. Abre la discusión que te interese.
                4. Lee comentarios, responde o interactúa según lo que esté habilitado.
                5. Si quieres, también puedes crear una nueva publicación desde la sección correspondiente.
                """.trim());
        }

        if (containsAny(message, "crear publicacion", "crear publicación", "crear post", "publicar en community")) {
            return simpleReply("""
                Paso a paso para crear una publicación en community:

                1. Entra a community.
                2. Ve a la opción de crear publicación.
                3. Escribe el contenido o tema que quieres compartir.
                4. Revisa que el texto esté claro.
                5. Publica el contenido.
                """.trim());
        }

        if (containsAny(message, "inscribirme", "inscribirse", "me inscribo", "me uno a un evento", "unirme a un evento", "entrar a un evento")) {
            return simpleReply("""
                Paso a paso para inscribirte a un evento:

                1. Busca el evento en explorar o en el mapa.
                2. Entra al detalle del evento.
                3. Revisa fecha, hora, precio y ubicación.
                4. Pulsa el botón de inscripción o compra si está disponible.
                5. Completa el proceso y revisa la confirmación.
                """.trim());
        }

        if (containsAny(message, "pagar", "comprar entrada", "comprar un evento", "pago de evento")) {
            return simpleReply("""
                Paso a paso para pagar un evento en ParchaFace:

                1. Entra al detalle del evento.
                2. Revisa el valor y la información del evento.
                3. Pulsa la opción para comprar o pagar.
                4. Selecciona el método de pago disponible.
                5. Completa el proceso y verifica la confirmación final.
                """.trim());
        }

        if (containsAny(message, "comentario", "comentarios", "comentar", "dejar comentario", "poner comentario")) {
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

        if (containsAny(message, "preferencias", "mis gustos", "configurar preferencias", "editar preferencias")) {
            return simpleReply("""
                Paso a paso para configurar tus preferencias:

                1. Ve a la sección de preferencias.
                2. Selecciona las categorías que más te interesan.
                3. Guarda los cambios.
                4. Luego vuelve al asistente y pídele recomendaciones según tus gustos.
                """.trim());
        }

        if (containsAny(message, "login", "iniciar sesion", "iniciar sesión")) {
            return simpleReply("""
                Paso a paso para iniciar sesión en ParchaFace:

                1. Entra a la pantalla de login.
                2. Escribe tu correo y tu contraseña.
                3. Pulsa el botón para iniciar sesión.
                4. Si los datos son correctos, entrarás a tu cuenta.
                """.trim());
        }

        if (containsAny(message, "registrarme", "registro", "crear cuenta")) {
            return simpleReply("""
                Paso a paso para registrarte en ParchaFace:

                1. Entra a la pantalla de registro.
                2. Completa tus datos personales.
                3. Crea tu contraseña.
                4. Revisa la información.
                5. Envía el formulario para crear tu cuenta.
                """.trim());
        }

        if (containsAny(message, "olvidé mi contraseña", "olvide mi contraseña", "recuperar contraseña", "cambiar contraseña")) {
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

        if (containsAny(message, "perfil")) {
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

        if (containsAny(message, "mapa", "eventos", "evento", "explorar", "explore", "ver eventos", "planes")) {
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
            List<AssistantOptionDto> options = exactResults.stream()
                    .limit(5)
                    .map(event -> toOption(event, memory))
                    .toList();

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
            List<AssistantOptionDto> options = similarResults.stream()
                    .limit(5)
                    .map(event -> toOption(event, memory))
                    .toList();

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
                            new AssistantOptionDto("transport-si", "Sí, inclúyelo", "sí necesito transporte"),
                            new AssistantOptionDto("transport-no", "No, sin transporte", "no necesito transporte")
                    )
            );
        }

        if (memory.wantsFoodSuggestions == null) {
            memory.awaiting = Awaiting.FOOD;
            return new AssistantResponse(
                    "¿Quieres que también te sugiera lugares para comer antes o después del evento?",
                    List.of(),
                    List.of(
                            new AssistantOptionDto("food-si", "Sí, con comida cerca", "sí quiero lugares para comer"),
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
            List<AssistantOptionDto> options = exactResults.stream()
                    .limit(3)
                    .map(event -> toOption(event, memory))
                    .toList();

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
            List<AssistantOptionDto> options = similarResults.stream()
                    .limit(3)
                    .map(event -> toOption(event, memory))
                    .toList();

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

        List<AssistantOptionDto> options = results.stream()
                .limit(5)
                .map(event -> toOption(event, memory))
                .toList();

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
                List<AssistantOptionDto> options = results.stream()
                        .limit(5)
                        .map(event -> toOption(event, memory))
                        .toList();

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
        List<AssistantOptionDto> options = results.stream()
                .map(event -> toOption(event, memory))
                .toList();

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
                Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen|busca(?:me)?|mu[eé]strame|mostrame|recomi[eé]ndame|quiero)(?:\s+(?:alg[uú]n|alguna|alguito|algo|un|una))?\s*(?:evento|eventos|concierto|conciertos|fiesta|fiestas|show|shows|plan|planes)?\s*(?:de|sobre|con)\s+(.+)$"),
                Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen)(?:\s+(?:alg[uú]n|alguna|algo|un|una))?\s*(?:evento|eventos|concierto|conciertos|fiesta|fiestas|show|shows|plan|planes)\s+(.+)$"),
                Pattern.compile("(?i)(?:hay|habra|habrá|existe|tienen|busca(?:me)?|mu[eé]strame|mostrame|recomi[eé]ndame|quiero)(?:\s+(?:alg[uú]n|alguna|algo|un|una))?\s+(.+)$")
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
                "hay algun show de", "hay algún show de",
                "hay una", "hay algun", "hay algún", "existe una", "existe algun", "existe algún",
                "tienen una", "tienen algun", "tienen algún",
                "busca", "búscame", "muestrame", "muéstrame", "mostrame",
                "recomiendame algo de", "recomiéndame algo de");
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
        if (!cityOnly.isEmpty()) return sortEvents(cityOnly, memory, preferredCategories);

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
        List<AssistantOptionDto> options = top.stream()
                .map(event -> toOption(event, memory))
                .toList();

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

        Double userLat = toDouble(context.get("userLat"));
        Double userLng = toDouble(context.get("userLng"));
        Double mapCenterLat = toDouble(context.get("mapCenterLat"));
        Double mapCenterLng = toDouble(context.get("mapCenterLng"));

        if (userLat != null && userLng != null) {
            memory.referenceLat = userLat;
            memory.referenceLng = userLng;
            return;
        }

        if (mapCenterLat != null && mapCenterLng != null) {
            memory.referenceLat = mapCenterLat;
            memory.referenceLng = mapCenterLng;
        }
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
        return normalize(
                safe(event.getTitulo()) + " " +
                        safe(event.getDescripcion()) + " " +
                        safe(event.getCategoria()) + " " +
                        safe(event.getNombreLugar()) + " " +
                        safe(event.getUbicacion()) + " " +
                        safe(event.getDireccionCompleta()) + " " +
                        safe(event.getCiudad())
        );
    }

    private List<String> extractSearchKeywords(String raw) {
        String normalized = normalize(raw).replaceAll("[^a-z0-9ñ\s]", " ");
        String[] tokens = normalized.split("\s+");

        Set<String> stopWords = new HashSet<>(List.of(
                "quiero", "algo", "evento", "eventos", "recomiendame",
                "recomendar", "plan", "planes", "para", "con", "sin",
                "gratis", "pago", "de", "del", "la", "el", "en", "por",
                "mi", "mis", "un", "una", "que", "estoy", "esta", "pero",
                "dia", "clima", "frio", "fria", "caliente", "calor",
                "soleado", "soleada", "lluvia", "lloviendo", "armame",
                "armar", "hazme", "dame", "porfa", "entonces", "oe",
                "aja", "segun", "preferencias", "gustos", "hay",
                "existe", "tienen", "buscame", "muestrame", "mostrame",
                "algun", "alguna", "presupuesto", "hasta", "pesos", "rmame"
        ));

        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 4 && !stopWords.contains(token) && !token.matches("\\d+")) {
                keywords.add(token);
            }
        }

        return keywords;
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

        List<AssistantOptionDto> options = results.stream()
                .limit(5)
                .map(event -> toOption(event, memory))
                .toList();

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
                "estoy triste", "ando triste", "me siento triste",
                "estoy feliz", "ando feliz", "me siento feliz",
                "estoy aburrido", "estoy aburrida", "ando aburrido", "ando aburrida",
                "estoy estresado", "estoy estresada", "ando estresado", "ando estresada")
                && containsAny(message, "recomienda", "recomiendame", "recomiéndame", "que me recomiendas", "qué me recomiendas", "quiero algo");
    }

    private String detectMood(String message) {
        if (containsAny(message, "triste", "desanimado", "desanimada")) return "TRISTE";
        if (containsAny(message, "feliz", "contento", "contenta")) return "FELIZ";
        if (containsAny(message, "estresado", "estresada", "agotado", "agotada")) return "ESTRESADO";
        if (containsAny(message, "aburrido", "aburrida")) return "ABURRIDO";
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

        List<AssistantOptionDto> options = rankedByWeather.stream()
                .limit(5)
                .map(event -> toOption(event, memory))
                .toList();

        String reply = buildWeatherRecommendationReply(top, weather, normalizedMessage);

        memory.resetGuided();
        return new AssistantResponse(reply, List.of(), options);
    }

    private boolean isWeatherRecommendationIntent(String message) {
        boolean hasWeatherCue = containsAny(message,
                "esta lloviendo", "está lloviendo", "si llueve", "con lluvia", "lluvia",
                "dia soleado", "día soleado", "soleado", "soleada", "sol",
                "clima", "pronostico", "pronóstico",
                "frio", "fria", "fresco", "fresquito",
                "caliente", "calor", "calientico", "templado");

        boolean hasRecommendationCue = containsAny(message,
                "evento", "eventos", "recomienda", "recomiendame", "quiero", "muestrame", "buscame")
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
                    "gaming", "gamer", "videojuego", "videojuegos", "esports", "e sports",
                    "torneo gamer", "playstation", "xbox", "nintendo", "valorant", "fifa",
                    "fortnite", "league of legends", "lol"
            );
            case "musica" -> List.of(
                    "musica", "concierto", "conciertos", "show", "toque", "dj",
                    "banda", "artista", "festival"
            );
            case "fiestas" -> List.of(
                    "fiesta", "fiestas", "rumba", "parche", "farra", "after party", "party"
            );
            case "deporte" -> List.of(
                    "deporte", "deportes", "futbol", "partido", "torneo", "cancha", "ejercicio"
            );
            case "gastronomia" -> List.of(
                    "gastronomia", "comida", "comer", "cena", "almuerzo", "brunch",
                    "restaurante", "degustacion"
            );
            case "networking" -> List.of(
                    "networking", "negocios", "emprendedores", "contactos", "network"
            );
            case "arte" -> List.of(
                    "arte", "artistico", "galeria", "exposicion", "museo"
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
                "hazme un plan porfa",
                "armame un plan",
                "ármame un plan",
                "rmame un plan",
                "armame plan",
                "ármame plan",
                "armame algo",
                "ármame algo",
                "rmame algo",
                "necesito un plan",
                "quiero un plan",
                "me ayudas con un plan",
                "organizame un plan",
                "organízame un plan",
                "quiero salir",
                "quiero una fiesta",
                "quiero ir a una fiesta",
                "buscame un plan",
                "búscame un plan",
                "recomiendame un plan",
                "recomiéndame un plan",
                "que puedo hacer hoy",
                "qué puedo hacer hoy",
                "dame un plan",
                "dame ideas",
                "quiero algo para hacer",
                "plancito"
        );
    }

    private boolean isPreferencesRecommendationIntent(String message) {
        return containsAny(message,
                "segun mis preferencias",
                "según mis preferencias",
                "segun mis gustos",
                "según mis gustos",
                "que me recomiendas",
                "qué me recomiendas",
                "recomiendame",
                "recomiéndame",
                "algo para mi",
                "algo que me guste"
        );
    }

    private boolean isEventSearchIntent(String message) {
        return containsAny(message,
                "quiero un evento",
                "busco un evento",
                "evento de",
                "eventos de",
                "eventos en",
                "hay eventos en",
                "hay un evento de",
                "hay algun evento de",
                "hay algún evento de",
                "hay un concierto de",
                "hay algun concierto de",
                "hay algún concierto de",
                "fiesta",
                "futbol",
                "fútbol",
                "musica",
                "música",
                "gastronomia",
                "gastronomía",
                "gaming",
                "arte",
                "networking",
                "deporte",
                "deportes",
                "deportivo",
                "deportiva",
                "ver eventos",
                "algo para hacer",
                "algún evento",
                "algun evento",
                "algún concierto",
                "algun concierto",
                "recomiendame algo",
                "recomiéndame algo",
                "quiero hacer algo"
        );
    }

    private boolean isAboutParchaFaceQuestion(String message) {
        return containsAny(message,
                "que es parchaface",
                "qué es parchaface",
                "quien es parchaface",
                "quién es parchaface",
                "para que sirve parchaface",
                "para qué sirve parchaface",
                "de que trata parchaface",
                "de qué trata parchaface",
                "que hace parchaface",
                "qué hace parchaface"
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
                "ayúdame a"
        );
    }

    private boolean isGoToFeatureIntent(String message) {
        return containsAny(message,
                "llevame",
                "llévame",
                "llevarme",
                "abre",
                "abrime",
                "quiero ir",
                "quiero entrar",
                "ir al",
                "ir a",
                "ve al",
                "ve a",
                "muestrame",
                "muéstrame",
                "mostrame",
                "muestre",
                "mandame",
                "mándame",
                "vamos al",
                "vamos a"
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
                "que hay disponible",
                "qué hay disponible",
                "mostrar eventos",
                "muestrame eventos",
                "muéstrame eventos",
                "listar eventos",
                "lista de eventos",
                "eventos disponibles",
                "hay eventos en",
                "si hay eventos en",
                "sí hay eventos en",
                "que hay en",
                "qué hay en",
                "hay algo en");
    }

    private boolean isPaymentMethodsQuestion(String message) {
        return containsAny(message,
                "metodos de pago",
                "métodos de pago",
                "como puedo pagar",
                "cómo puedo pagar",
                "con que puedo pagar",
                "con qué puedo pagar",
                "formas de pago"
        );
    }

    private boolean isTermsQuestion(String message) {
        return containsAny(message,
                "terminos y condiciones",
                "términos y condiciones",
                "terminos",
                "términos"
        );
    }

    private boolean isConductRulesQuestion(String message) {
        return containsAny(message,
                "grosero",
                "groserias",
                "groserías",
                "insultar",
                "insultos",
                "ofensivo",
                "ofensiva",
                "reglas de convivencia",
                "reglas de respeto",
                "reglas de comportamiento",
                "que no esta permitido",
                "qué no está permitido",
                "acoso",
                "amenazas",
                "spam",
                "lenguaje inapropiado");
    }

    private String buildConductRulesReply() {
        return """
                Sí. En ParchaFace no deberías publicar ni enviar contenido ofensivo, insultos, amenazas, acoso, discriminación, fraude, spam o información engañosa.
                Tampoco deberías usar lenguaje grosero para atacar a otras personas, publicar eventos falsos o compartir contenido ilegal.
                Si alguien incumple estas reglas, el contenido puede ser reportado, removido y la cuenta puede ser suspendida o bloqueada por administración.
                """.trim();
    }

    private boolean isPrivacyQuestion(String message) {
        return containsAny(message,
                "politica de privacidad",
                "política de privacidad",
                "privacidad",
                "datos personales"
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
                "cómo me voy"
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
                "línea 123"
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
                "dónde están los comentarios"
        );
    }

    private boolean seemsRelevantToParchaFace(String message) {
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
                "jaja"
        );
    }

    private boolean isDirectManipulationRequest(String message) {
        return containsAny(message,
                "creame un evento",
                "créame un evento",
                "crea un evento por mi",
                "crea un evento por mí",
                "hazme un evento",
                "editalo por mi",
                "edítalo por mí",
                "edita el evento por mi",
                "edita el evento por mí",
                "publicalo por mi",
                "publícalo por mí");
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
                .replaceAll("[^a-z0-9ñ\s]", " ")
                .replaceAll("\s+", " ")
                .trim();

        return Pattern.compile(
                "\b(?:hp|hpta|hptas|hijueputa|hijo de puta|hija de puta|jueputa|puta|puto|putita|putito|gonorrea|gono|carechimba|caremonda|malparido|malparida|marica|maricon|mariquita|pendejo|pendeja|cabron|cabrona|huevon|huevona|wevon|webon|guevon|culero|culera|culiado|culiada|mierda|imbecil|idiota|estupido|tarado|mamaguevo|mamahuevo|comemierda|pirobo|zorra|perra|coño|carajo|chingar|chingada|chingado|pinche|pelotudo|pelotuda|boludo|boluda|verga|fuck|fucking|fucker|motherfucker|mother fucker|shit|bullshit|asshole|ass hole|bitch|son of a bitch|bastard|slut|whore|dick|cock|pussy|cunt|retard|dumbass|jackass|piece of shit)\b"
        ).matcher(normalized).find();
    }

    private String detectCategory(String message) {
        String normalized = normalize(message);

        if (containsAny(normalized,
                "futbol", "fútbol", "deporte", "deportes",
                "deportivo", "deportiva", "deporticvo", "partido", "cancha",
                "ejercicio", "competencia", "torneo")) {
            return "DEPORTE";
        }

        if (containsAny(normalized,
                "musica", "música", "concierto", "conciertos",
                "show", "toque", "presentacion", "presentación",
                "dj", "banda", "festival")) {
            return "MUSICA";
        }

        if (containsAny(normalized,
                "arte", "artistico", "artístico", "exposicion", "exposición")) {
            return "ARTE";
        }

        if (containsAny(normalized,
                "gastronomia", "gastronomía", "comida", "comer",
                "restaurante", "cenar", "almorzar", "brunch")) {
            return "GASTRONOMIA";
        }

        if (containsAny(normalized,
                "networking", "emprendedores", "contactos", "negocios")) {
            return "NETWORKING";
        }

        if (containsAny(normalized,
                "gaming", "videojuegos", "videojuego", "juegos", "esports", "e-sports",
                "fortnite", "fifa", "valorant", "lol", "league of legends",
                "playstation", "xbox", "nintendo")) {
            return "GAMING";
        }

        if (containsAny(normalized,
                "fiesta", "fiestas", "rumba", "rumbita", "plancito", "parche", "farra",
                "poolparty", "pool party", "sintetica", "sintética", "electronica",
                "electrónica", "techno", "house", "after party")) {
            return "FIESTAS";
        }

        return null;
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

        Matcher matcher = Pattern.compile("(?i)\b(?:en|por|desde|cerca de|cerca a|a)\s+([a-zA-Záéíóúñ\s]{3,40})").matcher(message);
        while (matcher.find()) {
            String raw = matcher.group(1)
                    .replaceAll("(?i)\b(gratis|de pago|pago|pagado|a las|con|sin|para|porfa|por favor|y|pero|si|sí|si es|quiero|necesito|evento|eventos|azar|aleatorio|random|ramdom)\b.*$", "")
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

        if (containsAny(normalized, "zona norte", "norte")) return "norte";
        if (containsAny(normalized, "zona sur", "sur")) return "sur";
        if (containsAny(normalized, "zona centro", "centro")) return "centro";
        if (containsAny(normalized, "oriente")) return "oriente";
        if (containsAny(normalized, "occidente")) return "occidente";

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

        if (containsAny(normalized, "barato", "economico", "económico", "bajo")) return 20000;
        if (containsAny(normalized, "medio")) return 50000;
        if (containsAny(normalized, "alto")) return 100000;

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
        if (containsAny(normalized, "si", "sí", "claro", "de una", "quiero", "dale")) {
            return true;
        }

        if (containsAny(normalized, "no", "sin eso", "no quiero", "no necesito")) {
            return false;
        }

        return null;
    }

    private Boolean detectTransportPreference(String message) {
        String normalized = normalize(message);

        if (containsAny(normalized,
                "sin transporte", "no transporte", "no quiero transporte", "no necesito transporte",
                "sin taxi", "sin uber", "sin didi", "sin indrive", "sin picap")) {
            return false;
        }

        if (containsAny(normalized,
                "con transporte", "incluye transporte", "quiero transporte", "necesito transporte",
                "con taxi", "con uber", "con didi", "con indrive", "con picap")) {
            return true;
        }

        if (containsAny(normalized, "transporte", "taxi", "uber", "didi", "indrive", "picap")) {
            return true;
        }

        return null;
    }

    private Boolean detectFoodPreference(String message) {
        String normalized = normalize(message);

        if (containsAny(normalized,
                "sin comida", "no comida", "no quiero comida", "no quiero restaurantes", "sin restaurantes",
                "sin comer", "sin cena", "sin almuerzo", "sin brunch")) {
            return false;
        }

        if (containsAny(normalized,
                "con comida", "quiero comida", "quiero restaurantes", "quiero comer",
                "con restaurantes", "con cena", "con almuerzo", "con brunch")) {
            return true;
        }

        if (containsAny(normalized, "comer", "restaurante", "comida", "cenar", "almorzar", "brunch")) {
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

        for (String candidate : candidates) {
            String normalizedCandidate = normalize(candidate);

            if (normalizedSource.contains(normalizedCandidate)) {
                return true;
            }

            if (!normalizedCandidate.contains(" ")
                    && normalizedCandidate.length() >= 5
                    && fuzzyTokenMatch(normalizedSource, normalizedCandidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean fuzzyTokenMatch(String source, String candidate) {
        String[] tokens = source.split("[^a-z0-9ñ]+");
        int maxDistance = candidate.length() >= 8 ? 2 : 1;

        for (String token : tokens) {
            if (token.isBlank() || token.length() < 4) continue;
            if (editDistance(token, candidate) <= maxDistance) {
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
                .replaceAll("\bq\b", "que")
                .replaceAll("\bk\b", "que")
                .replaceAll("\bxq\b", "porque")
                .replaceAll("\bpa\b", "para")
                .replaceAll("\bpal\b", "para el")
                .replaceAll("\bpal\s+", "para el ")
                .replaceAll("\bparce\b", "amigo")
                .replaceAll("\bparcero\b", "amigo")
                .replaceAll("\bbro\b", "amigo")
                .replaceAll("\bmano\b", "amigo")
                .replaceAll("\bqlq\b", "que lo que")
                .replaceAll("\bklk\b", "que lo que")
                .replaceAll("[^a-z0-9ñ\s]", " ")
                .replaceAll("\s+", " ")
                .trim();

        return normalized;
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
                "explícame"
        );
    }
}