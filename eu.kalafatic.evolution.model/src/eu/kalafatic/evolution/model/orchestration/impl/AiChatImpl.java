/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.ChatThread;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Ai Chat</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getUrl <em>Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getToken <em>Token</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getPrompt <em>Prompt</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getProxyUrl <em>Proxy Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getThreads <em>Threads</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl#getPromptInstructions <em>Prompt Instructions</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AiChatImpl extends MinimalEObjectImpl.Container implements AiChat {
	/**
	 * The default value of the '{@link #getUrl() <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getUrl() <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUrl()
	 * @generated
	 * @ordered
	 */
	protected String url = URL_EDEFAULT;

	/**
	 * The default value of the '{@link #getToken() <em>Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getToken()
	 * @generated
	 * @ordered
	 */
	protected static final String TOKEN_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getToken() <em>Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getToken()
	 * @generated
	 * @ordered
	 */
	protected String token = TOKEN_EDEFAULT;

	/**
	 * The default value of the '{@link #getPrompt() <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPrompt()
	 * @generated
	 * @ordered
	 */
	protected static final String PROMPT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPrompt() <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPrompt()
	 * @generated
	 * @ordered
	 */
	protected String prompt = PROMPT_EDEFAULT;

	/**
	 * The default value of the '{@link #getProxyUrl() <em>Proxy Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProxyUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String PROXY_URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getProxyUrl() <em>Proxy Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProxyUrl()
	 * @generated
	 * @ordered
	 */
	protected String proxyUrl = PROXY_URL_EDEFAULT;

	/**
	 * The cached value of the '{@link #getThreads() <em>Threads</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreads()
	 * @generated
	 * @ordered
	 */
	protected EList<ChatThread> threads;

	/**
	 * The cached value of the '{@link #getPromptInstructions() <em>Prompt Instructions</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPromptInstructions()
	 * @generated
	 * @ordered
	 */
	protected PromptInstructions promptInstructions;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AiChatImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.AI_CHAT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getUrl() {
		return url;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUrl(String newUrl) {
		String oldUrl = url;
		url = newUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_CHAT__URL, oldUrl, url));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getToken() {
		return token;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setToken(String newToken) {
		String oldToken = token;
		token = newToken;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_CHAT__TOKEN, oldToken, token));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPrompt() {
		return prompt;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPrompt(String newPrompt) {
		String oldPrompt = prompt;
		prompt = newPrompt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_CHAT__PROMPT, oldPrompt, prompt));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getProxyUrl() {
		return proxyUrl;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setProxyUrl(String newProxyUrl) {
		String oldProxyUrl = proxyUrl;
		proxyUrl = newProxyUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_CHAT__PROXY_URL, oldProxyUrl, proxyUrl));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ChatThread> getThreads() {
		if (threads == null) {
			threads = new EObjectContainmentEList<ChatThread>(ChatThread.class, this, OrchestrationPackage.AI_CHAT__THREADS);
		}
		return threads;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public PromptInstructions getPromptInstructions() {
		if (promptInstructions != null && promptInstructions.eIsProxy()) {
			InternalEObject oldPromptInstructions = (InternalEObject)promptInstructions;
			promptInstructions = (PromptInstructions)eResolveProxy(oldPromptInstructions);
			if (promptInstructions != oldPromptInstructions) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS, oldPromptInstructions, promptInstructions));
			}
		}
		return promptInstructions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PromptInstructions basicGetPromptInstructions() {
		return promptInstructions;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPromptInstructions(PromptInstructions newPromptInstructions) {
		PromptInstructions oldPromptInstructions = promptInstructions;
		promptInstructions = newPromptInstructions;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS, oldPromptInstructions, promptInstructions));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.AI_CHAT__THREADS:
				return ((InternalEList<?>)getThreads()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.AI_CHAT__URL:
				return getUrl();
			case OrchestrationPackage.AI_CHAT__TOKEN:
				return getToken();
			case OrchestrationPackage.AI_CHAT__PROMPT:
				return getPrompt();
			case OrchestrationPackage.AI_CHAT__PROXY_URL:
				return getProxyUrl();
			case OrchestrationPackage.AI_CHAT__THREADS:
				return getThreads();
			case OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS:
				if (resolve) return getPromptInstructions();
				return basicGetPromptInstructions();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.AI_CHAT__URL:
				setUrl((String)newValue);
				return;
			case OrchestrationPackage.AI_CHAT__TOKEN:
				setToken((String)newValue);
				return;
			case OrchestrationPackage.AI_CHAT__PROMPT:
				setPrompt((String)newValue);
				return;
			case OrchestrationPackage.AI_CHAT__PROXY_URL:
				setProxyUrl((String)newValue);
				return;
			case OrchestrationPackage.AI_CHAT__THREADS:
				getThreads().clear();
				getThreads().addAll((Collection<? extends ChatThread>)newValue);
				return;
			case OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS:
				setPromptInstructions((PromptInstructions)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.AI_CHAT__URL:
				setUrl(URL_EDEFAULT);
				return;
			case OrchestrationPackage.AI_CHAT__TOKEN:
				setToken(TOKEN_EDEFAULT);
				return;
			case OrchestrationPackage.AI_CHAT__PROMPT:
				setPrompt(PROMPT_EDEFAULT);
				return;
			case OrchestrationPackage.AI_CHAT__PROXY_URL:
				setProxyUrl(PROXY_URL_EDEFAULT);
				return;
			case OrchestrationPackage.AI_CHAT__THREADS:
				getThreads().clear();
				return;
			case OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS:
				setPromptInstructions((PromptInstructions)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.AI_CHAT__URL:
				return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
			case OrchestrationPackage.AI_CHAT__TOKEN:
				return TOKEN_EDEFAULT == null ? token != null : !TOKEN_EDEFAULT.equals(token);
			case OrchestrationPackage.AI_CHAT__PROMPT:
				return PROMPT_EDEFAULT == null ? prompt != null : !PROMPT_EDEFAULT.equals(prompt);
			case OrchestrationPackage.AI_CHAT__PROXY_URL:
				return PROXY_URL_EDEFAULT == null ? proxyUrl != null : !PROXY_URL_EDEFAULT.equals(proxyUrl);
			case OrchestrationPackage.AI_CHAT__THREADS:
				return threads != null && !threads.isEmpty();
			case OrchestrationPackage.AI_CHAT__PROMPT_INSTRUCTIONS:
				return promptInstructions != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (url: ");
		result.append(url);
		result.append(", token: ");
		result.append(token);
		result.append(", prompt: ");
		result.append(prompt);
		result.append(", proxyUrl: ");
		result.append(proxyUrl);
		result.append(')');
		return result.toString();
	}

} //AiChatImpl
